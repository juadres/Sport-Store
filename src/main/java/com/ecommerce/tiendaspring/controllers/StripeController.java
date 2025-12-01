package com.ecommerce.tiendaspring.controllers;

import com.ecommerce.tiendaspring.models.*;
import com.ecommerce.tiendaspring.services.*;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/stripe")
public class StripeController {
    
    @Autowired
    private StripeService stripeService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private VentaService ventaService;
    
    @Autowired
    private CarritoService carritoService;
    
    /**
     * Página de pago con Stripe
     */
@PostMapping("/pago-stripe")
public String mostrarPagoStripe(Authentication authentication,
                               HttpSession session,  // Añadir HttpSession
                               Model model,
                               HttpServletResponse response) {
    
    try {
        // 1. Validar autenticación
        if (authentication == null || !authentication.isAuthenticated()) {
            if (!response.isCommitted()) {
                return "redirect:/login?redirect=/carrito";
            }
            return null;
        }
        
        // 2. Obtener carrito de la SESIÓN (no de @ModelAttribute)
        List<CarritoItemDTO> carrito = (List<CarritoItemDTO>) session.getAttribute("carrito");
        if (carrito == null || carrito.isEmpty()) {
            if (!response.isCommitted()) {
                return "redirect:/carrito?error=carrito_vacio";
            }
            return null;
        }
        
        // 3. Obtener usuario
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // 4. Calcular totales
        BigDecimal subtotal = carrito.stream()
            .map(CarritoItemDTO::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva = subtotal.multiply(new BigDecimal("0.19"));
        BigDecimal total = subtotal.add(iva);
        
        // 5. Crear referencia
        String referenciaVenta = "STR-" + System.currentTimeMillis();
        
        // 6. Crear PaymentIntent
        Map<String, String> resultado = stripeService.crearPaymentIntent(total, email, referenciaVenta);
        
        // 7. Pasar datos a la vista
        model.addAttribute("total", total);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("iva", iva);
        model.addAttribute("email", email);
        model.addAttribute("clientSecret", resultado.get("clientSecret"));
        model.addAttribute("stripePublicKey", stripeService.getPublicKey());
        model.addAttribute("referencia", referenciaVenta);
        
        // 8. Guardar en sesión temporal
        session.setAttribute("stripe_payment_intent", resultado.get("paymentIntentId"));
        session.setAttribute("stripe_email", email);
        
        return "pago-stripe";
        
    } catch (Exception e) {
        e.printStackTrace();
        if (!response.isCommitted()) {
            return "redirect:/carrito?error=stripe_init";
        }
        return null;
    }
}
    
    /**
     * Procesar pago exitoso - Usar POST para recibir datos del formulario
     */
    @PostMapping("/procesar-exito")
    @ResponseBody
    public Map<String, Object> procesarExito(@RequestParam String paymentIntentId,
                                            @RequestParam String email,
                                            @RequestParam String referencia,
                                            @RequestParam String carritoJson,
                                            HttpSession session) {
        
        Map<String, Object> respuesta = new HashMap<>();
        
        try {
            System.out.println("=== PROCESANDO PAGO STRIPE ===");
            System.out.println("PaymentIntent: " + paymentIntentId);
            System.out.println("Email: " + email);
            
            // 1. Verificar pago en Stripe
            boolean pagoOk = stripeService.verificarPaymentIntent(paymentIntentId);
            if (!pagoOk) {
                respuesta.put("success", false);
                respuesta.put("error", "Pago no verificado en Stripe");
                return respuesta;
            }
            
            // 2. Obtener usuario
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // 3. Obtener carrito de la sesión actual (NO de JSON por ahora)
            List<CarritoItemDTO> carrito = (List<CarritoItemDTO>) session.getAttribute("carrito");
            if (carrito == null || carrito.isEmpty()) {
                respuesta.put("success", false);
                respuesta.put("error", "Carrito no encontrado en sesión");
                return respuesta;
            }
            
            // 4. Crear venta usando método existente
            Venta venta = ventaService.procesarVentaStripe(usuario, carrito, paymentIntentId);
            
            // 5. Limpiar carrito
            carrito.clear();
            
            // 6. Limpiar carrito en BD usando nuevo método
            carritoService.limpiarCarritoUsuario(email);
            
            // 7. Respuesta exitosa
            respuesta.put("success", true);
            respuesta.put("ventaId", venta.getId());
            respuesta.put("numeroFactura", venta.getNumeroFactura());
            respuesta.put("mensaje", "¡Pago exitoso! Factura #" + venta.getNumeroFactura());
            
            System.out.println("=== PAGO PROCESADO EXITOSAMENTE ===");
            
        } catch (Exception e) {
            e.printStackTrace();
            respuesta.put("success", false);
            respuesta.put("error", "Error: " + e.getMessage());
        }
        
        return respuesta;
    }
    
    /**
     * Cancelar pago
     */
    @GetMapping("/cancelar")
    public String cancelarPago() {
        return "redirect:/carrito";
    }
}