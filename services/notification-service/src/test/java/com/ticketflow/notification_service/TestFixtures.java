package com.ticketflow.notification_service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ticketflow.notification_service.config.NotificationProperties;
import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;

public final class TestFixtures {

    public static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private TestFixtures() {
    }

    public static NotificationProperties properties(int maxAttempts) {
        return new NotificationProperties(
                new NotificationProperties.Delivery(maxAttempts, Duration.ofSeconds(30), 50),
                new NotificationProperties.Email("log", "no-reply@ticketflow.local"),
                new NotificationProperties.Kafka("notification-service", ".DLT", 3, 1000, List.of("t")));
    }

    public static EventEnvelope envelope(String type, Map<String, Object> payload) {
        return new EventEnvelope(UUID.randomUUID(), type, 1, NOW, "booking-42", "corr-1", payload);
    }

    public static Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    public static Notification pendingNotification() {
        return Notification.pending(UUID.randomUUID(), "BookingConfirmed", "booking-42", "corr-1",
                "alice@example.com", NotificationChannel.EMAIL, "Sujet", "Corps", NOW, NOW);
    }
}
