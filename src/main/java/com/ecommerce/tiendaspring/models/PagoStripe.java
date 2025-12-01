package com.ecommerce.tiendaspring.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_stripe")
public class PagoStripe {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)  // MODIFICADO: Quitado nullable = false
    private String stripeSessionId;
    
    @Column(unique = true)
    private String stripePaymentIntentId;
    
    @OneToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;
    
    @Column(nullable = false, length = 20)
    private String estado;
    
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;
    
    @Column
    private LocalDateTime fechaActualizacion;
    
    @Column(length = 100)
    private String emailCliente;
    
    @Column(length = 100)
    private String referenciaVenta;
    
    // Constructor vacío
    public PagoStripe() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "pendiente";
    }
    
    // Constructor para PaymentIntent
    public PagoStripe(String stripePaymentIntentId, BigDecimal montoTotal, 
                     String emailCliente, String referenciaVenta) {
        this();
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.montoTotal = montoTotal;
        this.emailCliente = emailCliente;
        this.referenciaVenta = referenciaVenta;
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
    
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { 
        this.stripePaymentIntentId = stripePaymentIntentId; 
    }
    
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { 
        this.venta = venta; 
        this.fechaActualizacion = LocalDateTime.now();
    }
    
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { 
        this.estado = estado; 
        this.fechaActualizacion = LocalDateTime.now();
    }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    
    public String getEmailCliente() { return emailCliente; }
    public void setEmailCliente(String emailCliente) { this.emailCliente = emailCliente; }
    
    public String getReferenciaVenta() { return referenciaVenta; }
    public void setReferenciaVenta(String referenciaVenta) { this.referenciaVenta = referenciaVenta; }
}