package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.PagoStripe;
import com.ecommerce.tiendaspring.models.Venta;
import com.ecommerce.tiendaspring.repositories.PagoStripeRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class StripeService {
    
    @Value("${stripe.secret.key}")
    private String secretKey;
    
    @Value("${stripe.public.key}")
    private String publicKey;
    
    @Autowired
    private PagoStripeRepository pagoStripeRepository;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    /**
     * Crear PaymentIntent para pago embebido
     */
    public Map<String, String> crearPaymentIntent(BigDecimal total, String email, String referenciaVenta) 
            throws StripeException {
        
        // Convertir a centavos (Stripe trabaja en la menor unidad monetaria)
        long amount = total.multiply(new BigDecimal("100")).longValue();
        
        // Crear parámetros del PaymentIntent
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency("cop") // Para Colombia
            .setDescription("Compra SportStore - Ref: " + referenciaVenta)
            .putMetadata("referencia", referenciaVenta)
            .putMetadata("email", email)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
            .build();
        
        // Crear PaymentIntent
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        
        // Guardar en base de datos - CORREGIDO
        PagoStripe pago = new PagoStripe();
        pago.setMontoTotal(total);
        pago.setEmailCliente(email);
        pago.setReferenciaVenta(referenciaVenta);
        pago.setStripePaymentIntentId(paymentIntent.getId());  // IMPORTANTE: Guardar el ID
        pago.setEstado("pendiente");
        pagoStripeRepository.save(pago);
        
        System.out.println("=== PAYMENT INTENT CREADO ===");
        System.out.println("ID: " + paymentIntent.getId());
        System.out.println("Cliente: " + email);
        System.out.println("Monto: " + total + " COP (" + amount + " centavos)");
        System.out.println("Estado: " + paymentIntent.getStatus());
        
        // Retornar datos necesarios
        Map<String, String> resultado = new HashMap<>();
        resultado.put("paymentIntentId", paymentIntent.getId());
        resultado.put("clientSecret", paymentIntent.getClientSecret());
        
        return resultado;
    }
    
    /**
     * Verificar estado del pago
     */
    public boolean verificarPaymentIntent(String paymentIntentId) throws StripeException {
        System.out.println("Verificando PaymentIntent: " + paymentIntentId);
        
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        
        System.out.println("Estado obtenido: " + paymentIntent.getStatus());
        System.out.println("Monto: " + paymentIntent.getAmount());
        System.out.println("Moneda: " + paymentIntent.getCurrency());
        
        // Actualizar estado en BD
        PagoStripe pago = pagoStripeRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> {
                    System.out.println("Pago no encontrado en BD: " + paymentIntentId);
                    return new RuntimeException("Pago no encontrado");
                });
        
        String estado = paymentIntent.getStatus();
        pago.setEstado(estado);
        pagoStripeRepository.save(pago);
        
        System.out.println("Estado actualizado en BD: " + estado);
        
        return "succeeded".equals(estado);
    }
    
    /**
     * Asociar venta a pago
     */
    public void asociarVentaAPago(String paymentIntentId, Venta venta) {
        System.out.println("Asociando venta a pago: " + paymentIntentId);
        
        PagoStripe pago = pagoStripeRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> {
                    System.out.println("Pago no encontrado para asociar: " + paymentIntentId);
                    return new RuntimeException("Pago no encontrado");
                });
        
        pago.setVenta(venta);
        pago.setEstado("completado");
        pagoStripeRepository.save(pago);
        
        System.out.println("Venta asociada: " + venta.getId() + " - " + venta.getNumeroFactura());
    }
    
    /**
     * Cancelar pago pendiente
     */
    public boolean cancelarPaymentIntent(String paymentIntentId) throws StripeException {
        System.out.println("Intentando cancelar PaymentIntent: " + paymentIntentId);
        
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            System.out.println("Estado actual: " + paymentIntent.getStatus());
            
            // Solo cancelar si está pendiente
            if ("requires_payment_method".equals(paymentIntent.getStatus()) || 
                "requires_confirmation".equals(paymentIntent.getStatus())) {
                
                PaymentIntent cancelled = paymentIntent.cancel();
                
                System.out.println("Cancelación exitosa. Nuevo estado: " + cancelled.getStatus());
                
                // Actualizar en BD
                PagoStripe pago = pagoStripeRepository.findByStripePaymentIntentId(paymentIntentId)
                        .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
                pago.setEstado("cancelado");
                pagoStripeRepository.save(pago);
                
                return true;
            } else {
                System.out.println("No se puede cancelar - Estado: " + paymentIntent.getStatus());
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("Error al cancelar PaymentIntent: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Obtener pago por PaymentIntent ID
     */
    public PagoStripe obtenerPagoPorPaymentIntentId(String paymentIntentId) {
        return pagoStripeRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
    }
    
    /**
     * Obtener pago por referencia
     */
    public PagoStripe obtenerPagoPorReferencia(String referenciaVenta) {
        return pagoStripeRepository.findByReferenciaVenta(referenciaVenta)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
    }
    
    /**
     * Verificar si un pago ya existe para evitar duplicados
     */
    public boolean existePagoParaReferencia(String referenciaVenta) {
        return pagoStripeRepository.findByReferenciaVenta(referenciaVenta).isPresent();
    }
}