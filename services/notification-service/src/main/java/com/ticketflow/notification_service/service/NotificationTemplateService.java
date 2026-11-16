package com.ticketflow.notification_service.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.exceptions.InvalidEventException;

/** Construit sujet et corps (texte brut, français) à partir du type d'événement et du payload. */
@Service
public class NotificationTemplateService {

    public record Content(String subject, String message) {
    }

    private static final int MAX_VALUE = 200;
    private static final int MAX_SUBJECT = 200;
    private static final int MAX_BODY = 5000;
    private static final String SIGNATURE = "\n\nL'équipe TicketFlow";
    private static final String PREFERENCES_HINT =
            "\n\nVous pouvez gérer vos préférences de notification depuis votre espace TicketFlow.";

    public Content render(SupportedEventType type, EventEnvelope envelope) {
        Map<String, Object> p = envelope.payload() == null ? Map.of() : envelope.payload();
        String title = value(p, "eventTitle", "votre événement");
        String booking = value(p, "bookingId", envelope.aggregateId() == null ? "" : envelope.aggregateId());
        String bookingRef = booking.isBlank() ? "" : " " + booking;

        return switch (type) {
            case BOOKING_CONFIRMED -> new Content(
                    "Réservation confirmée : " + title,
                    "Bonjour,\n\nVotre réservation" + bookingRef + " pour « " + title + " » est confirmée."
                            + dateLine(p)
                            + "\n\nVos billets sont disponibles dans votre espace TicketFlow." + SIGNATURE);
            case BOOKING_EXPIRED -> new Content(
                    "Réservation expirée : " + title,
                    "Bonjour,\n\nVotre réservation" + bookingRef + " pour « " + title
                            + " » a expiré faute de paiement dans le délai imparti. Les places ont été libérées."
                            + "\n\nVous pouvez effectuer une nouvelle réservation si des places sont encore disponibles."
                            + SIGNATURE);
            case PAYMENT_SUCCEEDED -> new Content(
                    "Paiement reçu",
                    "Bonjour,\n\nNous avons bien reçu votre paiement" + amountPart(p)
                            + referencePart(booking) + "." + SIGNATURE);
            case PAYMENT_FAILED -> new Content(
                    "Échec du paiement",
                    "Bonjour,\n\nVotre paiement" + amountPart(p) + referencePart(booking)
                            + " n'a pas abouti. Votre réservation n'est pas confirmée."
                            + "\n\nVous pouvez réessayer tant que la réservation n'a pas expiré." + SIGNATURE);
            case TICKET_ISSUED -> new Content(
                    "Votre billet est disponible : " + title,
                    "Bonjour,\n\nVotre billet pour « " + title + " » a été émis." + dateLine(p)
                            + "\n\nRetrouvez-le dans votre espace TicketFlow." + SIGNATURE);
            case EVENT_PUBLISHED -> new Content(
                    "Nouvel événement : " + title,
                    "Bonjour,\n\nUn nouvel événement vient d'être publié : « " + title + " »." + dateLine(p)
                            + "\n\nConsultez le catalogue TicketFlow pour les détails." + PREFERENCES_HINT + SIGNATURE);
            case EVENT_CLOSED -> new Content(
                    "Événement clôturé : " + title,
                    "Bonjour,\n\nL'événement « " + title + " » est clôturé ou a été modifié de façon importante."
                            + dateLine(p) + "\n\nConsultez votre espace TicketFlow pour plus d'informations."
                            + PREFERENCES_HINT + SIGNATURE);
            case NOTIFICATION_REQUESTED -> custom(p);
        };
    }

    private Content custom(Map<String, Object> p) {
        String subject = clean(p.get("subject"), MAX_SUBJECT);
        String message = cleanBody(p.get("message"));
        if (subject.isBlank() || message.isBlank()) {
            throw new InvalidEventException("NotificationRequested : subject et message sont obligatoires");
        }
        return new Content(subject, message + PREFERENCES_HINT);
    }

    private static String dateLine(Map<String, Object> p) {
        String date = value(p, "eventDate", "");
        return date.isBlank() ? "" : "\nDate : " + date + ".";
    }

    private static String amountPart(Map<String, Object> p) {
        String amount = value(p, "amount", "");
        if (amount.isBlank()) {
            return "";
        }
        return " de " + amount + " " + value(p, "currency", "");
    }

    private static String referencePart(String booking) {
        return booking.isBlank() ? "" : " pour la réservation " + booking;
    }

    private static String value(Map<String, Object> p, String key, String fallback) {
        String v = clean(p.get(key), MAX_VALUE);
        return v.isBlank() ? fallback : v;
    }

    /** Supprime les caractères de contrôle (dont CR/LF : injection d'en-têtes) et borne la longueur. */
    static String clean(Object raw, int max) {
        if (raw == null) {
            return "";
        }
        String s = String.valueOf(raw).replaceAll("\\p{Cntrl}", " ").trim();
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Comme clean, mais conserve les retours à la ligne du corps. */
    static String cleanBody(Object raw) {
        if (raw == null) {
            return "";
        }
        String s = String.valueOf(raw).replaceAll("[\\p{Cntrl}&&[^\\n]]", " ").trim();
        return s.length() <= MAX_BODY ? s : s.substring(0, MAX_BODY);
    }
}
