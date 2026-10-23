package com.ticketflow.event_service.controllers;

import com.ticketflow.event_service.dto.EventRequest;
import com.ticketflow.event_service.dto.EventResponse;
import com.ticketflow.event_service.service.EventService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final int MAX_PAGE_SIZE = 50;

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        EventResponse created = EventResponse.from(service.create(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    PagedModel<EventResponse> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("eventDate"));
        return new PagedModel<>(service.listPublished(pageable).map(EventResponse::from));
    }

    @GetMapping("/{id}")
    EventResponse get(@PathVariable Long id) {
        return EventResponse.from(service.getPublished(id));
    }

    @PostMapping("/{id}/publish")
    EventResponse publish(@PathVariable Long id) {
        return EventResponse.from(service.publish(id));
    }
}