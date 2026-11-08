package com.ticketflow.event_service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketflow.event_service.exceptions.EventStateException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventLifecycleTest {

    private Event draft() {
        return new Event("Concert", "Resume", Instant.parse("2027-06-15T20:00:00Z"),
                "Casablanca", "Salle", "Musique", new BigDecimal("50.00"), 100);
    }

    private void update(Event e, int capacity) {
        e.update("Concert", "Resume", Instant.parse("2027-06-15T20:00:00Z"),
                "Casablanca", "Salle", "Musique", new BigDecimal("50.00"), capacity);
    }

    @Test
    void publishOnlyFromDraft() {
        Event e = draft();
        e.publish();
        assertThat(e.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThatThrownBy(e::publish).isInstanceOf(EventStateException.class);
    }

    @Test
    void closeOnlyWhenPublished() {
        Event e = draft();
        assertThatThrownBy(e::close).isInstanceOf(EventStateException.class);
        e.publish();
        e.close();
        assertThat(e.getStatus()).isEqualTo(EventStatus.CLOSED);
    }

    @Test
    void archiveFromDraftOrClosedOnly() {
        Event published = draft();
        published.publish();
        assertThatThrownBy(published::archive).isInstanceOf(EventStateException.class);

        published.close();
        published.archive();
        assertThat(published.getStatus()).isEqualTo(EventStatus.ARCHIVED);

        Event d = draft();
        d.archive();
        assertThat(d.getStatus()).isEqualTo(EventStatus.ARCHIVED);
    }

    @Test
    void closedOrArchivedCannotBeUpdated() {
        Event e = draft();
        e.publish();
        e.close();
        assertThatThrownBy(() -> update(e, 100)).isInstanceOf(EventStateException.class);
    }

    @Test
    void publishedCapacityCanGrowButNotShrink() {
        Event e = draft();
        e.publish();
        update(e, 150);
        assertThat(e.getCapacity()).isEqualTo(150);
        assertThatThrownBy(() -> update(e, 120)).isInstanceOf(EventStateException.class);
    }

    @Test
    void draftCapacityCanShrink() {
        Event e = draft();
        update(e, 20);
        assertThat(e.getCapacity()).isEqualTo(20);
    }
}