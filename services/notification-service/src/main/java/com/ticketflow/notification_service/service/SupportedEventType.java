package com.ticketflow.notification_service.service;

import java.util.Optional;
import java.util.Set;

/** Types d'événements qui produisent une notification. */
public enum SupportedEventType {

    EVENT_PUBLISHED("EventPublished", false, false),
    EVENT_CLOSED("EventClosed", false, false),
    BOOKING_EXPIRED("BookingExpired", true, true),
    BOOKING_CONFIRMED("BookingConfirmed", true, true),
    PAYMENT_SUCCEEDED("PaymentSucceeded", true, true),
    PAYMENT_FAILED("PaymentFailed", true, true),
    TICKET_ISSUED("TicketIssued", true, true),
    NOTIFICATION_REQUESTED("NotificationRequested", true, false);

    /** Types connus du contrat mais volontairement sans notification (consommés puis ignorés). */
    private static final Set<String> KNOWN_IGNORED = Set.of("BookingCreated");

    private final String wireName;
    private final boolean recipientRequired;
    private final boolean transactional;

    SupportedEventType(String wireName, boolean recipientRequired, boolean transactional) {
        this.wireName = wireName;
        this.recipientRequired = recipientRequired;
        this.transactional = transactional;
    }

    public String wireName() {
        return wireName;
    }

    /** Si vrai, l'événement doit porter au moins un destinataire, sinon il est invalide. */
    public boolean recipientRequired() {
        return recipientRequired;
    }

    /** Les notifications transactionnelles ne peuvent pas être désactivées par les préférences. */
    public boolean transactional() {
        return transactional;
    }

    public static Optional<SupportedEventType> from(String wireName) {
        for (SupportedEventType t : values()) {
            if (t.wireName.equals(wireName)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public static boolean isKnownIgnored(String wireName) {
        return KNOWN_IGNORED.contains(wireName);
    }
}
