package com.ticketflow.event_service.controllers;

import com.ticketflow.event_service.dto.EventRequest;
import com.ticketflow.event_service.dto.EventResponse;
import com.ticketflow.event_service.entities.EventStatus;
import com.ticketflow.event_service.service.EventSearchCriteria;
import com.ticketflow.event_service.service.EventService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Set<String> SORTABLE = Set.of("eventDate", "price", "title");

    private final EventService service;
    private final int maxPageSize;

    public EventController(EventService service,
                           @Value("${ticketflow.events.max-page-size:50}") int maxPageSize) {
        this.service = service;
        this.maxPageSize = maxPageSize;
    }

    // ---------- public ----------

    @GetMapping
    PagedModel<EventResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "eventDate") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (q != null && q.length() > 100) {
            throw badRequest("q : 100 caractères maximum");
        }
        if (minPrice != null && minPrice.signum() < 0) {
            throw badRequest("minPrice doit être positif ou nul");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw badRequest("minPrice ne peut pas dépasser maxPrice");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw badRequest("dateFrom ne peut pas être après dateTo");
        }
        if (!SORTABLE.contains(sort)) {
            throw badRequest("sort doit valoir : eventDate, price ou title");
        }
        Sort.Direction dir = switch (direction.toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw badRequest("direction doit valoir asc ou desc");
        };

        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), maxPageSize),
                Sort.by(dir, sort).and(Sort.by("id")));

        EventSearchCriteria criteria = new EventSearchCriteria(q, city, category,
                dateFrom == null ? null : dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant(),
                dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                minPrice, maxPrice);

        return new PagedModel<>(service.search(criteria, pageable).map(EventResponse::from));
    }

    @GetMapping("/{id}")
    EventResponse get(@PathVariable Long id) {
        return EventResponse.from(service.getPublished(id));
    }

    // ---------- gestion : ORGANIZER, sur ses propres événements ----------

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        EventResponse created = EventResponse.from(service.create(request, jwt.getSubject()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request,
                         @AuthenticationPrincipal Jwt jwt) {
        return EventResponse.from(service.update(id, request, jwt.getSubject()));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse publish(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return EventResponse.from(service.publish(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse close(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return EventResponse.from(service.close(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse archive(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return EventResponse.from(service.archive(id, jwt.getSubject()));
    }

    // Vue de gestion : mes événements, tous statuts (brouillons compris)
    @GetMapping("/manage")
    @PreAuthorize("hasRole('ORGANIZER')")
    PagedModel<EventResponse> manage(@RequestParam(required = false) EventStatus status,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @AuthenticationPrincipal Jwt jwt) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), maxPageSize),
                Sort.by("createdAt").descending().and(Sort.by("id")));
        return new PagedModel<>(service.listForManagement(jwt.getSubject(), status, pageable)
                .map(EventResponse::from));
    }

    @GetMapping("/manage/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    EventResponse manageGet(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return EventResponse.from(service.getForManagement(id, jwt.getSubject()));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}