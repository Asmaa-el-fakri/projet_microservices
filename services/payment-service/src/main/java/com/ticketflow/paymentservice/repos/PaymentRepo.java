package com.ticketflow.paymentservice.repos;

import com.ticketflow.paymentservice.entities.Payment;
import com.ticketflow.paymentservice.entities.PaymentStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepo extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndOwnerId(Long id, String ownerId);

    boolean existsByBookingIdAndStatusIn(Long bookingId, Collection<PaymentStatus> statuses);
}