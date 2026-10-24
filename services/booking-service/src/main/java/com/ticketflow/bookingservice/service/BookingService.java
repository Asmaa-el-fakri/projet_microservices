package com.ticketflow.bookingservice.service;

import com.ticketflow.bookingservice.dto.BookingRequest;
import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.repos.BookingRepo;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private final BookingRepo repo;

    public BookingService(BookingRepo repo) {
        this.repo = repo;
    }

    @Transactional
    public Booking create(BookingRequest r) {
        // TP4 : vérifier le client (customer-service) et l'événement (event-service) avec OpenFeign
        return repo.save(new Booking(r.customerId(), r.eventId(), r.quantity()));
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