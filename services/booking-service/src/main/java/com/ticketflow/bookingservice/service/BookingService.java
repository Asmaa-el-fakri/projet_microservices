package com.ticketflow.bookingservice.service;

import com.ticketflow.bookingservice.clients.EventClient;
import com.ticketflow.bookingservice.clients.EventView;
import com.ticketflow.bookingservice.dto.BookingRequest;
import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.repos.BookingRepo;
import feign.FeignException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepo repo;
    private final EventClient eventClient;

    public BookingService(BookingRepo repo, EventClient eventClient) {
        this.repo = repo;
        this.eventClient = eventClient;
    }

    // Pas de @Transactional ici : on ne garde pas de transaction ouverte
    // pendant l'attente de la réponse d'event-service. Le save() a sa propre transaction.
    public Booking create(BookingRequest r) {
        EventView event = fetchEvent(r.eventId());
        log.info("Réservation pour l'événement {} ({})", event.id(), event.title());
        return repo.save(new Booking(r.customerId(), r.eventId(), r.quantity()));
    }

    private EventView fetchEvent(Long eventId) {
        try {
            return eventClient.getById(eventId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Événement " + eventId + " introuvable");
        } catch (FeignException e) {
            log.warn("Appel à event-service en échec : status={}", e.status());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "event-service est indisponible");
        }
    }

    @Transactional(readOnly = true)
    public Booking get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @Transactional(readOnly = true)
    public List<Booking> listByCustomer(Long customerId) {
        return repo.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}