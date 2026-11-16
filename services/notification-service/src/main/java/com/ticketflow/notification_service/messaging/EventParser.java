package com.ticketflow.notification_service.messaging;

import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.exceptions.InvalidEventException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Désérialise l'enveloppe JSON. Tout message illisible devient une InvalidEventException (-> DLT). */
@Component
public class EventParser {

    private static final int MAX_MESSAGE_BYTES = 64 * 1024;

    private final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public EventEnvelope parse(String json) {
        if (json == null || json.isBlank()) {
            throw new InvalidEventException("Message vide");
        }
        if (json.length() > MAX_MESSAGE_BYTES) {
            throw new InvalidEventException("Message trop volumineux");
        }
        try {
            EventEnvelope envelope = mapper.readValue(json, EventEnvelope.class);
            if (envelope == null) {
                throw new InvalidEventException("Enveloppe vide");
            }
            return envelope;
        } catch (JacksonException e) {
            // Le détail peut contenir des fragments du message : on ne garde que le type d'erreur.
            throw new InvalidEventException("JSON invalide : " + e.getClass().getSimpleName(), e);
        }
    }
}
