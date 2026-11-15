
package com.ticketflow.ticketservice.controllers;

import com.ticketflow.ticketservice.dto.IssueTicketRequest;
import com.ticketflow.ticketservice.dto.TicketResponse;
import com.ticketflow.ticketservice.service.TicketService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TicketResponse> issue(
            @Valid @RequestBody IssueTicketRequest request) {
        return ticketService.issue(request);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return ticketService.get(id);
    }

    @GetMapping("/booking/{bookingId}")
    public List<TicketResponse> getByBooking(
            @PathVariable Long bookingId) {
        return ticketService.getByBooking(bookingId);
    }

    @GetMapping("/verify/{code}")
    public TicketResponse verify(@PathVariable String code) {
        return ticketService.verify(code);
    }

    @PatchMapping("/{id}/use")
    public TicketResponse markAsUsed(@PathVariable Long id) {
        return ticketService.markAsUsed(id);
    }

    @PatchMapping("/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id) {
        return ticketService.cancel(id);
    }
}
