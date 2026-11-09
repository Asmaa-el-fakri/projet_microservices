package com.ticketflow.paymentservice.dto;

import com.ticketflow.paymentservice.entities.Payment;
import com.ticketflow.paymentservice.entities.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long bookingId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getBookingId(), p.getAmount(), p.getCurrency(),
                p.getStatus(), p.getCreatedAt(), p.getUpdatedAt());
    }
}