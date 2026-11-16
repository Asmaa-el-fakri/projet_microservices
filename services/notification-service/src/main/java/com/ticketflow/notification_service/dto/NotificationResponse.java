package com.ticketflow.notification_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.ticketflow.notification_service.entities.Notification;

/** recipient est null pour l'utilisateur qui consulte ses propres notifications. */
public record NotificationResponse(
        UUID id,
        UUID eventId,
        String eventType,
        String channel,
        String recipient,
        String subject,
        String status,
        int attemptCount,
        String provider,
        Instant createdAt,
        Instant sentAt,
        String lastError,
        String correlationId) {

    public static NotificationResponse forOwner(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventId(), n.getEventType(), n.getChannel().name(), null,
                n.getSubject(), n.getStatus().name(), n.getAttemptCount(), n.getProvider(), n.getCreatedAt(),
                n.getSentAt(), null, n.getCorrelationId());
    }

    public static NotificationResponse forAdmin(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventId(), n.getEventType(), n.getChannel().name(),
                n.getRecipient(), n.getSubject(), n.getStatus().name(), n.getAttemptCount(), n.getProvider(),
                n.getCreatedAt(), n.getSentAt(), n.getLastError(), n.getCorrelationId());
    }
}
