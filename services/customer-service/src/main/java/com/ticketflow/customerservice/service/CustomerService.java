package com.ticketflow.customerservice.service;

import com.ticketflow.customerservice.dto.CustomerRequest;
import com.ticketflow.customerservice.dto.CustomerUpdateRequest;
import com.ticketflow.customerservice.entities.AuditAction;
import com.ticketflow.customerservice.entities.Customer;
import com.ticketflow.customerservice.exceptions.CustomerNotFoundException;
import com.ticketflow.customerservice.exceptions.DuplicateEmailException;
import com.ticketflow.customerservice.repos.CustomerRepo;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private static final String RESOURCE = "Customer";

    private final CustomerRepo repo;
    private final AuditService audit;

    public CustomerService(CustomerRepo repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    // ---------- administration (ADMIN), journalisee ----------

    public Customer create(CustomerRequest request) {
        String email = normalize(request.email());
        if (repo.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        Customer saved = repo.save(new Customer(
                request.firstName(), request.lastName(), email, request.phone()));
        audit.record(AuditAction.CUSTOMER_CREATED, RESOURCE, String.valueOf(saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return repo.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Customer> list() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Customer> listClosureRequested() {
        return repo.findByClosureRequestedTrue();
    }

    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        String email = normalize(request.email());
        if (!customer.getEmail().equals(email) && repo.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        customer.update(request.firstName(), request.lastName(), email, request.phone());
        audit.record(AuditAction.CUSTOMER_UPDATED, RESOURCE, String.valueOf(id));
        return customer;
    }

    public void delete(Long id) {
        repo.delete(get(id));
        audit.record(AuditAction.CUSTOMER_DELETED, RESOURCE, String.valueOf(id));
    }

    // ---------- FR-005 : profil connecte via Keycloak ----------

    // Creation automatique du profil a la premiere connexion (le "sub" du JWT)
    @Transactional
    public Customer getOrCreateProfile(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return repo.findByKeycloakId(keycloakId).orElseGet(() -> {
            Customer c = new Customer(
                    jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"),
                    normalize(jwt.getClaimAsString("email")),
                    null // phone : renseigne plus tard par le client
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

    // Demande de fermeture de compte (FR-005), visible ensuite par l'administrateur
    public void requestClosure(Jwt jwt) {
        Customer customer = getOrCreateProfile(jwt);
        customer.setClosureRequested(true);
        repo.save(customer);
        audit.record(AuditAction.CLOSURE_REQUESTED, RESOURCE, String.valueOf(customer.getId()));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}