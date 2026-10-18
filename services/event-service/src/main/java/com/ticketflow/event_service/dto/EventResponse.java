package com.ticketflow.event_service.dto;

import com.ticketflow.event_service.entities.Event;
import com.ticketflow.event_service.entities.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record EventResponse(
        Long id,
        String title,
        String summary,
        Instant eventDate,
        String city,
        String venue,
        String category,
        BigDecimal price,
        int capacity,
        EventStatus status) {

    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getTitle(), e.getSummary(), e.getEventDate(),
                e.getCity(), e.getVenue(), e.getCategory(), e.getPrice(),
                e.getCapacity(), e.getStatus());
    }
}