package com.ticketflow.event_service.service;

import com.ticketflow.event_service.entities.Event;
import com.ticketflow.event_service.entities.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class EventSpecifications {

    private EventSpecifications() {
    }

    static Specification<Event> from(EventSearchCriteria c) {
        List<Specification<Event>> parts = new ArrayList<>();

        // Un brouillon n'est jamais visible publiquement : condition toujours appliquée
        parts.add((root, query, cb) -> cb.equal(root.get("status"), EventStatus.PUBLISHED));

        if (hasText(c.q())) {
            String pattern = "%" + escape(c.q().trim().toLowerCase(Locale.ROOT)) + "%";
            parts.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("summary")), pattern, '\\')));
        }
        if (hasText(c.city())) {
            String city = c.city().trim().toLowerCase(Locale.ROOT);
            parts.add((root, query, cb) -> cb.equal(cb.lower(root.<String>get("city")), city));
        }
        if (hasText(c.category())) {
            String category = c.category().trim().toLowerCase(Locale.ROOT);
            parts.add((root, query, cb) -> cb.equal(cb.lower(root.<String>get("category")), category));
        }
        if (c.dateFrom() != null) {
            Instant from = c.dateFrom();
            parts.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("eventDate"), from));
        }
        if (c.dateToExclusive() != null) {
            Instant to = c.dateToExclusive();
            parts.add((root, query, cb) -> cb.lessThan(root.<Instant>get("eventDate"), to));
        }
        if (c.minPrice() != null) {
            BigDecimal min = c.minPrice();
            parts.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), min));
        }
        if (c.maxPrice() != null) {
            BigDecimal max = c.maxPrice();
            parts.add((root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), max));
        }
        return Specification.allOf(parts);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // Les caractères % et _ saisis par l'utilisateur sont cherchés tels quels, pas comme jokers
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}