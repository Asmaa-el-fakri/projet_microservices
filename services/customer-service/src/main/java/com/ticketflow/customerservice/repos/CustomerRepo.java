package com.ticketflow.customerservice.repos;

import com.ticketflow.customerservice.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepo extends JpaRepository<Customer, Long> {
    boolean existsByEmail(String email);

    // Nouvelle méthode pour FR-005
    Optional<Customer> findByKeycloakId(String keycloakId);
}