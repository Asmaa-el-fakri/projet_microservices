package com.ticketflow.paymentservice.events;

import com.ticketflow.paymentservice.entities.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Evenement de domaine publie sur Kafka (voir docs/EVENTS.md).
// Enveloppe des fiches du cours : eventId, eventType, version, occurredAt, data ;
// plus aggregateId et correlationId (cahier des charges, section 7.3).
public record PaymentEvent(
        String eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        Data data) {

    public static final int SCHEMA_VERSION = 1;
    public static final String TYPE_SUCCEEDED = "PaymentSucceeded";
    public static final String TYPE_FAILED = "PaymentFailed";

    // customerRef = "sub" du jeton Keycloak du client (identifiant opaque, pas de donnee personnelle)
    public record Data(
            Long paymentId,
            Long bookingId,
            String customerRef,
            BigDecimal amount,
            String currency,
            String reason) {
    }

    public static PaymentEvent of(Payment p) {
        return switch (p.getStatus()) {
            case SUCCEEDED -> build(p, TYPE_SUCCEEDED, null);
            case FAILED -> build(p, TYPE_FAILED, "FAILURE");
            case CANCELLED -> build(p, TYPE_FAILED, "CANCELLED");
            case PENDING -> throw new IllegalArgumentException(
                    "Un paiement en attente ne produit pas d'evenement");
        };
    }

    private static PaymentEvent build(Payment p, String type, String reason) {
        Data data = new Data(p.getId(), p.getBookingId(), p.getOwnerId(),
                p.getAmount(), p.getCurrency(), reason);
        return new PaymentEvent(UUID.randomUUID().toString(), type, SCHEMA_VERSION, p.getUpdatedAt(),
                String.valueOf(p.getId()), "booking-" + p.getBookingId(), data);
    }
}