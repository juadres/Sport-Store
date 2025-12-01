package com.ecommerce.tiendaspring.repositories;

import com.ecommerce.tiendaspring.models.PagoStripe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoStripeRepository extends JpaRepository<PagoStripe, Long> {
    Optional<PagoStripe> findByStripeSessionId(String sessionId);
    Optional<PagoStripe> findByStripePaymentIntentId(String paymentIntentId);
    Optional<PagoStripe> findByVentaId(Long ventaId);
    Optional<PagoStripe> findByReferenciaVenta(String referenciaVenta);
}