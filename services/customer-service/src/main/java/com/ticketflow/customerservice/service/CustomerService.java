package com.ticketflow.customerservice.service;

import com.ticketflow.customerservice.dto.CustomerRequest;
import com.ticketflow.customerservice.dto.CustomerUpdateRequest;
import com.ticketflow.customerservice.entities.Customer;
import com.ticketflow.customerservice.exceptions.CustomerNotFoundException;
import com.ticketflow.customerservice.exceptions.DuplicateEmailException;
import com.ticketflow.customerservice.repos.CustomerRepo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepo repo;

    public CustomerService(CustomerRepo repo) {
        this.repo = repo;
    }

    // ---------- méthodes existantes (inchangées) ----------

    public Customer create(CustomerRequest request) {
        String email = normalize(request.email());
        if (repo.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        return repo.save(new Customer(
                request.firstName(), request.lastName(), email, request.phone()));
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return repo.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Customer> list() {
        return repo.findAll();
    }

    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        String email = normalize(request.email());
        if (!customer.getEmail().equals(email) && repo.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        customer.update(request.firstName(), request.lastName(), email, request.phone());
        return customer;
    }

    public void delete(Long id) {
        repo.delete(get(id));
    }

    // ---------- FR-005 : profil connecté via Keycloak ----------

    // Création automatique du profil à la première connexion (le "sub" du JWT)
    @Transactional
    public Customer getOrCreateProfile(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return repo.findByKeycloakId(keycloakId).orElseGet(() -> {
            Customer c = new Customer(
                    jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"),
                    normalize(jwt.getClaimAsString("email")),
                    null // phone : renseigné plus tard par le client
            );
            c.setKeycloakId(keycloakId);
            return repo.save(c);
        });
    }

    // Seuls firstName, lastName, phone sont modifiables par le client (FR-005)
    public Customer updateMyProfile(Jwt jwt, CustomerUpdateRequest request) {
        Customer customer = getOrCreateProfile(jwt);
        customer.update(request.firstName(), request.lastName(),
                customer.getEmail(), // email jamais modifiable ici
                request.phone());
        return customer;
    }

    // Demande de fermeture de compte (FR-005)
    public void requestClosure(Jwt jwt) {
        Customer customer = getOrCreateProfile(jwt);
        customer.setClosureRequested(true);
        repo.save(customer);
        // plus tard (livraison) : notifier un admin via Kafka (partie C)
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}