
package com.ticketflow.ticketservice.service;

import com.ticketflow.ticketservice.dto.IssueTicketRequest;
import com.ticketflow.ticketservice.dto.TicketResponse;
import com.ticketflow.ticketservice.entities.Ticket;
import com.ticketflow.ticketservice.entities.TicketStatus;
import com.ticketflow.ticketservice.repos.TicketRepo;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private final TicketRepo repo;
    private final SecureRandom secureRandom = new SecureRandom();

    public TicketService(TicketRepo repo) {
        this.repo = repo;
    }

    // Issue tickets for a confirmed booking
    @Transactional
    public List<TicketResponse> issue(IssueTicketRequest request) {

        if (request.quantity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket quantity must be greater than zero"
            );
        }

        // Prevent duplicate tickets when Kafka redelivers an event
        List<Ticket> existingTickets =
                repo.findByBookingId(request.bookingId());

        if (!existingTickets.isEmpty()) {

            if (existingTickets.size() != request.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Existing ticket quantity does not match the booking"
                );
            }

            return existingTickets.stream()
                    .map(TicketResponse::from)
                    .toList();
        }

        List<Ticket> tickets = new ArrayList<>();

        for (int i = 0; i < request.quantity(); i++) {
            String code = generateVerificationCode();

            Ticket ticket = new Ticket(
                    request.bookingId(),
                    request.customerId(),
                    code
            );

            tickets.add(ticket);
        }

        return repo.saveAll(tickets)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    // Get a ticket by its ID
    @Transactional(readOnly = true)
    public TicketResponse get(Long id) {

        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        return TicketResponse.from(ticket);
    }

    // Get all tickets belonging to a booking
    @Transactional(readOnly = true)
    public List<TicketResponse> getByBooking(Long bookingId) {

        return repo.findByBookingId(bookingId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    // Verify a ticket using its verification code
    @Transactional(readOnly = true)
    public TicketResponse verify(String code) {

        Ticket ticket = repo.findByVerificationCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invalid ticket code"
                ));

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ticket is not valid. Current status: "
                            + ticket.getStatus()
            );
        }

        return TicketResponse.from(ticket);
    }

    // Mark a valid ticket as used when its holder enters the event
    @Transactional
    public TicketResponse markAsUsed(Long id) {

        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only valid tickets can be used"
            );
        }

        ticket.markAsUsed();

        return TicketResponse.from(ticket);
    }

    // Cancel a valid ticket
    @Transactional
    public TicketResponse cancel(Long id) {

        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only valid tickets can be cancelled"
            );
        }

        ticket.cancel();

        return TicketResponse.from(ticket);
    }

    // Generate a secure, unpredictable verification code
    private String generateVerificationCode() {

        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
