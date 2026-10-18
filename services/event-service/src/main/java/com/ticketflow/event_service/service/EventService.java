package com.ticketflow.event_service.service;

import com.ticketflow.event_service.dto.EventRequest;
import com.ticketflow.event_service.entities.Event;
import com.ticketflow.event_service.entities.EventStatus;
import com.ticketflow.event_service.repos.EventRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {

    private final EventRepo repo;

    public EventService(EventRepo repo) {
        this.repo = repo;
    }

    @Transactional
    public Event create(EventRequest r) {
        Event event = new Event(r.title(), r.summary(), r.eventDate(), r.city(),
                r.venue(), r.category(), r.price(), r.capacity());
        return repo.save(event);
    }

    @Transactional(readOnly = true)
    public Page<Event> listPublished(Pageable pageable) {
        return repo.findByStatus(EventStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Event getPublished(Long id) {
        return repo.findById(id)
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @Transactional
    public Event publish(Long id) {
        Event event = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        event.publish();
        return event;
    }
}