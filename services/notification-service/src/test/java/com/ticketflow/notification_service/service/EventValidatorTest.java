package com.ticketflow.notification_service.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ticketflow.notification_service.TestFixtures;
import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.exceptions.InvalidEventException;

class EventValidatorTest {

    private final EventValidator validator = new EventValidator();

    @Test
    void acceptsValidEnvelope() {
        assertThatCode(() -> validator.validate(TestFixtures.envelope("BookingConfirmed", Map.of())))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingEventId() {
        EventEnvelope e = new EventEnvelope(null, "BookingConfirmed", 1, TestFixtures.NOW, "a", "c", Map.of());
        assertThatThrownBy(() -> validator.validate(e)).isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("eventId");
    }

    @Test
    void rejectsBlankType() {
        EventEnvelope e = new EventEnvelope(UUID.randomUUID(), " ", 1, TestFixtures.NOW, "a", "c", Map.of());
        assertThatThrownBy(() -> validator.validate(e)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void rejectsUnsupportedVersion() {
        EventEnvelope e = new EventEnvelope(UUID.randomUUID(), "BookingConfirmed", 2, TestFixtures.NOW, "a", "c", Map.of());
        assertThatThrownBy(() -> validator.validate(e)).isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("eventVersion");
    }

    @Test
    void rejectsMissingTimestamp() {
        EventEnvelope e = new EventEnvelope(UUID.randomUUID(), "BookingConfirmed", 1, null, "a", "c", Map.of());
        assertThatThrownBy(() -> validator.validate(e)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void rejectsNullEnvelope() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidEventException.class);
    }
}
