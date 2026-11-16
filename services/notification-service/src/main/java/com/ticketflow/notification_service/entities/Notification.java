package com.ticketflow.notification_service.entities;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "notifications")
public class Notification {

    private static final Duration MAX_BACKOFF = Duration.ofHours(1);
    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    private UUID id;

    @Version
    private long version;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", length = 100, updatable = false)
    private String aggregateId;

    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    @Column(nullable = false, updatable = false, length = 320)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, updatable = false)
    private String subject;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(length = 30)
    private String provider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    protected Notification() {
        // JPA
    }

    /** Nouvelle notification en attente ; la première tentative est planifiée à firstAttemptAt. */
    public static Notification pending(UUID eventId, String eventType, String aggregateId, String correlationId,
                                       String recipient, NotificationChannel channel, String subject,
                                       String message, Instant now, Instant firstAttemptAt) {
        Notification n = base(eventId, eventType, aggregateId, correlationId, recipient, channel, subject, message, now);
        n.status = NotificationStatus.PENDING;
        n.nextAttemptAt = firstAttemptAt;
        return n;
    }

    /** Notification conservée pour audit mais non envoyée (préférence du destinataire). */
    public static Notification skipped(UUID eventId, String eventType, String aggregateId, String correlationId,
                                       String recipient, NotificationChannel channel, String subject,
                                       String message, Instant now, String reason) {
        Notification n = base(eventId, eventType, aggregateId, correlationId, recipient, channel, subject, message, now);
        n.status = NotificationStatus.SKIPPED;
        n.lastError = truncate(reason);
        return n;
    }

    private static Notification base(UUID eventId, String eventType, String aggregateId, String correlationId,
                                     String recipient, NotificationChannel channel, String subject,
                                     String message, Instant now) {
        Notification n = new Notification();
        n.id = UUID.randomUUID();
        n.eventId = eventId;
        n.eventType = eventType;
        n.aggregateId = aggregateId;
        n.correlationId = correlationId;
        n.recipient = recipient;
        n.channel = channel;
        n.subject = subject;
        n.message = message;
        n.attemptCount = 0;
        n.createdAt = now;
        return n;
    }

    public void markSent(Instant now, String providerName) {
        this.attemptCount++;
        this.status = NotificationStatus.SENT;
        this.provider = providerName;
        this.sentAt = now;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    /** Enregistre un échec : nouvelle tentative avec backoff exponentiel, ou FAILED si le maximum est atteint. */
    public void markAttemptFailed(String error, Instant now, int maxAttempts, Duration baseDelay) {
        this.attemptCount++;
        this.lastError = truncate(error);
        if (this.attemptCount >= maxAttempts) {
            this.status = NotificationStatus.FAILED;
            this.nextAttemptAt = null;
        } else {
            this.status = NotificationStatus.PENDING;
            long factor = 1L << Math.min(this.attemptCount - 1, 20);
            Duration delay = baseDelay.multipliedBy(factor);
            if (delay.compareTo(MAX_BACKOFF) > 0) {
                delay = MAX_BACKOFF;
            }
            this.nextAttemptAt = now.plus(delay);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getCorrelationId() { return correlationId; }
    public String getRecipient() { return recipient; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public NotificationStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getProvider() { return provider; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
}
