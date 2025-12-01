package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.*;
import com.ecommerce.tiendaspring.repositories.VentaRepository;
import com.ecommerce.tiendaspring.repositories.DetalleVentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private StockService stockService;

    public Venta procesarVenta(Usuario usuario, List<CarritoItemDTO> carritoItems) {
        // Calcular totales
        BigDecimal subtotal = carritoItems.stream()
                .map(CarritoItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal iva = subtotal.multiply(new BigDecimal("0.19"));
        BigDecimal total = subtotal.add(iva);

        // Crear la venta
        Venta venta = new Venta(usuario, subtotal, total);
        ventaRepository.save(venta);

        // Crear detalles de venta y actualizar stock
        for (CarritoItemDTO item : carritoItems) {
            DetalleVenta detalle = new DetalleVenta(venta, item.getProducto(), item.getCantidad());
            venta.agregarDetalle(detalle);
            detalleVentaRepository.save(detalle);

            // Confirmar la venta en el stock
            stockService.confirmarVenta(item.getProducto().getId(), item.getCantidad());
        }

        return venta;
    }

    public List<Venta> obtenerVentasPorUsuario(Long usuarioId) {
        return ventaRepository.findByUsuarioIdOrderByFechaVentaDesc(usuarioId);
    }

    public List<Venta> obtenerTodasLasVentas() {
        return ventaRepository.findAllByOrderByFechaVentaDesc();
    }

    public Venta obtenerVentaPorId(Long id) {
        return ventaRepository.findById(id).orElse(null);
    }

    public List<DetalleVenta> obtenerDetallesVenta(Long ventaId) {
        return detalleVentaRepository.findByVentaId(ventaId);
    }
    
    // ================= NUEVO: Método para Stripe - Procesar venta con PaymentIntent =================
    public Venta procesarVentaStripe(Usuario usuario, List<CarritoItemDTO> carritoItems, 
                                    String stripePaymentIntentId) {
        System.out.println("Procesando venta Stripe para usuario: " + usuario.getEmail());
        
        // Usar el método existente para crear la venta
        Venta venta = procesarVenta(usuario, carritoItems);
        
        // Marcar como pagado con Stripe
        venta.setMetodoPago("stripe");
        venta.setStripePaymentIntentId(stripePaymentIntentId);
        venta.setEstadoPago("completado");
        
        // Guardar cambios
        ventaRepository.save(venta);
        
        System.out.println("Venta Stripe procesada: " + venta.getNumeroFactura() + 
                          " - PaymentIntent: " + stripePaymentIntentId);
        
        return venta;
    }
}