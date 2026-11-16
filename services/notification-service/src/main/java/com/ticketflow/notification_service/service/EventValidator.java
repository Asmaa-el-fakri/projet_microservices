package com.ticketflow.notification_service.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.exceptions.InvalidEventException;

/** Vérifie que l'enveloppe respecte le contrat. Toute violation lève InvalidEventException (-> DLT). */
@Component
public class EventValidator {

    public static final Set<Integer> SUPPORTED_VERSIONS = Set.of(1);

    private static final int MAX_FIELD_LENGTH = 100;

    public void validate(EventEnvelope e) {
        if (e == null) {
            throw new InvalidEventException("Enveloppe absente");
        }
        if (e.eventId() == null) {
            throw new InvalidEventException("eventId manquant");
        }
        if (isBlank(e.eventType())) {
            throw new InvalidEventException("eventType manquant");
        }
        if (e.eventVersion() == null || !SUPPORTED_VERSIONS.contains(e.eventVersion())) {
            throw new InvalidEventException("eventVersion non supportée : " + e.eventVersion());
        }
        if (e.occurredAt() == null) {
            throw new InvalidEventException("occurredAt manquant");
        }
        if (tooLong(e.eventType()) || tooLong(e.aggregateId()) || tooLong(e.correlationId())) {
            throw new InvalidEventException("Champ d'enveloppe trop long");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean tooLong(String s) {
        return s != null && s.length() > MAX_FIELD_LENGTH;
    }
}
