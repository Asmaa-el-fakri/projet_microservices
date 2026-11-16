package com.ticketflow.notification_service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.exceptions.InvalidEventException;

class EventParserTest {

    private final EventParser parser = new EventParser();

    @Test
    void parsesValidEnvelope() {
        String id = UUID.randomUUID().toString();
        String json = """
                {"eventId":"%s","eventType":"BookingConfirmed","eventVersion":1,
                 "occurredAt":"2026-11-15T12:00:00Z","aggregateId":"booking-42","correlationId":"corr-1",
                 "payload":{"recipientEmail":"alice@example.com","eventTitle":"Concert"}}
                """.formatted(id);

        EventEnvelope e = parser.parse(json);

        assertThat(e.eventId()).hasToString(id);
        assertThat(e.eventType()).isEqualTo("BookingConfirmed");
        assertThat(e.eventVersion()).isEqualTo(1);
        assertThat(e.occurredAt()).isEqualTo(Instant.parse("2026-11-15T12:00:00Z"));
        assertThat(e.aggregateId()).isEqualTo("booking-42");
        assertThat(e.payload()).containsEntry("eventTitle", "Concert");
    }

    @Test
    void ignoresUnknownProperties() {
        String json = """
                {"eventId":"%s","eventType":"EventClosed","eventVersion":1,
                 "occurredAt":"2026-11-15T12:00:00Z","futureField":"x","payload":{}}
                """.formatted(UUID.randomUUID());

        assertThat(parser.parse(json).eventType()).isEqualTo("EventClosed");
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.parse("{not json"))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void rejectsInvalidUuid() {
        assertThatThrownBy(() -> parser.parse("{\"eventId\":\"nope\",\"eventType\":\"X\"}"))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void rejectsEmptyOrNullMessage() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> parser.parse("  ")).isInstanceOf(InvalidEventException.class);
    }
}
