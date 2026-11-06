package com.game3alhaq.repository;

import com.game3alhaq.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaypalOrderId(String paypalOrderId);

    Optional<Payment> findByBookingId(Long bookingId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b JOIN FETCH b.hall h WHERE p.paypalOrderId = :paypalOrderId")
    Optional<Payment> findByPaypalOrderIdWithBooking(@Param("paypalOrderId") String paypalOrderId);
}
