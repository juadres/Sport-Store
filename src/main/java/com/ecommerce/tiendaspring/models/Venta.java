package com.ecommerce.tiendaspring.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroFactura;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaVenta;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private String estado;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    // Constructores
    public Venta() {
        this.fechaVenta = LocalDateTime.now();
        this.estado = "COMPLETADA";
        this.iva = new BigDecimal("0.19");
    }

    public Venta(Usuario usuario, BigDecimal subtotal, BigDecimal total) {
        this();
        this.usuario = usuario;
        this.subtotal = subtotal;
        this.total = total;
        this.iva = total.subtract(subtotal);
        this.numeroFactura = generarNumeroFactura();
    }

    private String generarNumeroFactura() {
        return "FAC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }

    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        this.detalles.add(detalle);
    }
// Campos para Stripe (se mantiene compatibilidad con pagos directos)
    @Column(length = 20)
    private String metodoPago = "directo";  // "directo" o "stripe"

    @Column(length = 100)
    private String stripeSessionId;

    @Column(length = 100)
    private String stripePaymentIntentId;

    @Column(length = 20)
    private String estadoPago = "completado";  // "pendiente", "completado", "fallido"

    @Column(length = 10)
    private String moneda = "COP";

    // Getters y Setters para los nuevos campos
    public String getMetodoPago() { 
        return metodoPago != null ? metodoPago : "directo"; 
    }
    public void setMetodoPago(String metodoPago) { 
        this.metodoPago = metodoPago; 
    }

    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { 
        this.stripeSessionId = stripeSessionId; 
    }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { 
        this.stripePaymentIntentId = stripePaymentIntentId; 
    }

    public String getEstadoPago() { 
        return estadoPago != null ? estadoPago : "completado"; 
    }
    public void setEstadoPago(String estadoPago) { 
        this.estadoPago = estadoPago; 
    }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

}