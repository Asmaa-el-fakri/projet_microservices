package com.ticketflow.ticketservice.repos;

import com.ticketflow.ticketservice.entities.Ticket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepo extends JpaRepository<Ticket, Long> {

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByCustomerIdOrderByIssuedAtDesc(Long customerId);

    Optional<Ticket> findByVerificationCode(String verificationCode);

    boolean existsByBookingId(Long bookingId);
}