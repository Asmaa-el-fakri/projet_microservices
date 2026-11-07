package com.ticketflow.event_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketflow.event_service.dto.EventRequest;
import com.ticketflow.event_service.entities.Event;
import com.ticketflow.event_service.entities.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class EventOwnershipTest {

    @Autowired
    EventService service;

    private EventRequest request(String title) {
        return new EventRequest(title, "Resume", Instant.parse("2030-01-01T10:00:00Z"),
                "Casablanca", "Salle", "Musique", new BigDecimal("50.00"), 100);
    }

    private void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void ownerCanManageHisEvent() {
        Event e = service.create(request("Mon evenement"), "orga-1");
        service.update(e.getId(), request("Titre modifie"), "orga-1");
        assertThat(service.publish(e.getId(), "orga-1").getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void anotherOrganizerCannotTouchIt() {
        Long id = service.create(request("Prive"), "orga-1").getId();
        assertNotFound(() -> service.update(id, request("Pirate"), "orga-2"));
        assertNotFound(() -> service.publish(id, "orga-2"));
        assertNotFound(() -> service.close(id, "orga-2"));
        assertNotFound(() -> service.archive(id, "orga-2"));
        assertNotFound(() -> service.getForManagement(id, "orga-2"));
    }

    @Test
    void managementListContainsOnlyOwnEvents() {
        service.create(request("A1"), "orga-1");
        service.create(request("B1"), "orga-2");
        Page<Event> page = service.listForManagement("orga-1", null, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Event::getTitle).containsExactly("A1");
    }
}