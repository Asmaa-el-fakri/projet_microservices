package com.ticketflow.notification_service.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Enveloppe versionnée de tous les événements de domaine consommés (contrat Kafka).
 * Les propriétés inconnues sont ignorées pour permettre des évolutions compatibles.
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        Map<String, Object> payload) {
}
