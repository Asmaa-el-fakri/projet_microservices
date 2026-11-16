package com.ticketflow.notification_service.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.exceptions.InvalidEventException;

/**
 * Détermine les destinataires à partir du payload : "recipientEmail" (un destinataire) ou
 * "recipientEmails" (liste). Le producteur de l'événement fournit l'adresse : le service
 * Notification ne duplique pas la base clients.
 */
@Component
public class RecipientResolver {

    static final int MAX_RECIPIENTS = 500;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@,;<>]+@[^\\s@,;<>]+\\.[^\\s@,;<>]+$");

    public List<String> resolve(SupportedEventType type, EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        Set<String> result = new LinkedHashSet<>();
        if (payload != null) {
            add(result, payload.get("recipientEmail"));
            if (payload.get("recipientEmails") instanceof List<?> list) {
                list.forEach(o -> add(result, o));
            }
        }
        if (result.size() > MAX_RECIPIENTS) {
            throw new InvalidEventException("Trop de destinataires (max " + MAX_RECIPIENTS + ")");
        }
        if (result.isEmpty() && type.recipientRequired()) {
            throw new InvalidEventException("Aucun destinataire valide pour " + type.wireName());
        }
        return List.copyOf(result);
    }

    private static void add(Set<String> target, Object value) {
        if (value == null) {
            return;
        }
        String email = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (email.length() > 320 || !EMAIL.matcher(email).matches()) {
            throw new InvalidEventException("Adresse destinataire invalide");
        }
        target.add(email);
    }
}
