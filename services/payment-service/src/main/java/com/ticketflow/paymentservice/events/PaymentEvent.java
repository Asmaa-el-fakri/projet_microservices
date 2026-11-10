package com.ticketflow.paymentservice.events;

import com.ticketflow.paymentservice.entities.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Evenement de domaine publie sur Kafka (identifiant unique, type, version, date UTC,
// agregat et correlation). Aucune donnee personnelle : pas d'identifiant du client.
public record PaymentEvent(
        String eventId,
        String type,
        int version,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        Long paymentId,
        Long bookingId,
        BigDecimal amount,
        String currency,
        String reason) {

    public static final int SCHEMA_VERSION = 1;

    public static PaymentEvent of(Payment p) {
        return switch (p.getStatus()) {
            case SUCCEEDED -> build(p, "PaymentSucceeded", null);
            case FAILED -> build(p, "PaymentFailed", "FAILURE");
            case CANCELLED -> build(p, "PaymentFailed", "CANCELLED");
            case PENDING -> throw new IllegalArgumentException(
                    "Un paiement en attente ne produit pas d'evenement");
        };
    }

    private static PaymentEvent build(Payment p, String type, String reason) {
        return new PaymentEvent(UUID.randomUUID().toString(), type, SCHEMA_VERSION, p.getUpdatedAt(),
                String.valueOf(p.getId()), "booking-" + p.getBookingId(),
                p.getId(), p.getBookingId(), p.getAmount(), p.getCurrency(), reason);
    }
}