package com.ticketflow.customerservice.repos;

import com.ticketflow.customerservice.entities.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepo extends JpaRepository<Customer, Long> {
    boolean existsByEmail(String email);

    // FR-005 : profil lie au jeton Keycloak
    Optional<Customer> findByKeycloakId(String keycloakId);

    // Administration : demandes de fermeture de compte en attente
    List<Customer> findByClosureRequestedTrue();
}