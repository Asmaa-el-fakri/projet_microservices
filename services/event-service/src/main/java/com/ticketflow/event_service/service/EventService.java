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
    public Page<Event> search(EventSearchCriteria criteria, Pageable pageable) {
        return repo.findAll(EventSpecifications.from(criteria), pageable);
    }

    @Transactional(readOnly = true)
    public Event getPublished(Long id) {
        return repo.findById(id)
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .orElseThrow(() -> notFound());
    }

    // --- gestion (tous statuts) ---

    @Transactional(readOnly = true)
    public Event getForManagement(Long id) {
        return find(id);
    }

    @Transactional(readOnly = true)
    public Page<Event> listForManagement(EventStatus status, Pageable pageable) {
        return status == null ? repo.findAll(pageable) : repo.findByStatus(status, pageable);
    }

    @Transactional
    public Event update(Long id, EventRequest r) {
        Event event = find(id);
        event.update(r.title(), r.summary(), r.eventDate(), r.city(),
                r.venue(), r.category(), r.price(), r.capacity());
        return event;
    }

    @Transactional
    public Event publish(Long id) {
        Event event = find(id);
        event.publish();
        return event;
    }

    @Transactional
    public Event close(Long id) {
        Event event = find(id);
        event.close();
        return event;
    }

    @Transactional
    public Event archive(Long id) {
        Event event = find(id);
        event.archive();
        return event;
    }

    private Event find(Long id) {
        return repo.findById(id).orElseThrow(() -> notFound());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
    }
}