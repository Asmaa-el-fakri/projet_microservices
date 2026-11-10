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

    // ---------- public ----------

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
                .orElseThrow(EventService::notFound);
    }

    // ---------- gestion : réservé au propriétaire ----------

    @Transactional
    public Event create(EventRequest r, String organizerId) {
        Event event = new Event(r.title(), r.summary(), r.eventDate(), r.city(),
                r.venue(), r.category(), r.price(), r.capacity());
        event.assignOrganizer(organizerId);
        return repo.save(event);
    }

    @Transactional(readOnly = true)
    public Event getForManagement(Long id, String organizerId) {
        return findOwned(id, organizerId);
    }

    @Transactional(readOnly = true)
    public Page<Event> listForManagement(String organizerId, EventStatus status, Pageable pageable) {
        return status == null
                ? repo.findByOrganizerId(organizerId, pageable)
                : repo.findByOrganizerIdAndStatus(organizerId, status, pageable);
    }

    @Transactional
    public Event update(Long id, EventRequest r, String organizerId) {
        Event event = findOwned(id, organizerId);
        event.update(r.title(), r.summary(), r.eventDate(), r.city(),
                r.venue(), r.category(), r.price(), r.capacity());
        return event;
    }

    @Transactional
    public Event publish(Long id, String organizerId) {
        Event event = findOwned(id, organizerId);
        event.publish();
        return event;
    }

    @Transactional
    public Event close(Long id, String organizerId) {
        Event event = findOwned(id, organizerId);
        event.close();
        return event;
    }

    @Transactional
    public Event archive(Long id, String organizerId) {
        Event event = findOwned(id, organizerId);
        event.archive();
        return event;
    }

    private Event findOwned(Long id, String organizerId) {
        return repo.findById(id)
                .filter(e -> e.isOwnedBy(organizerId))
                .orElseThrow(EventService::notFound);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
    }
}