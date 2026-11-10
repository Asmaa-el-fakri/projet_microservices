package com.ticketflow.customerservice.controllers;

import com.ticketflow.customerservice.dto.AuditLogResponse;
import com.ticketflow.customerservice.dto.CustomerRequest;
import com.ticketflow.customerservice.dto.CustomerResponse;
import com.ticketflow.customerservice.dto.CustomerUpdateRequest;
import com.ticketflow.customerservice.entities.Customer;
import com.ticketflow.customerservice.service.AuditService;
import com.ticketflow.customerservice.service.CustomerService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final int MAX_AUDIT_PAGE_SIZE = 100;

    private final CustomerService service;
    private final AuditService audit;

    public CustomerController(CustomerService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    // ========== FR-005 : Profil connecte ==========

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(CustomerResponse.from(service.getOrCreateProfile(jwt)));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(CustomerResponse.from(service.updateMyProfile(jwt, request)));
    }

    @PostMapping("/me/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> closeMe(@AuthenticationPrincipal Jwt jwt) {
        service.requestClosure(jwt);
        return ResponseEntity.accepted().build();
    }

    // ========== Administration (ADMIN) ==========

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = CustomerResponse.from(service.create(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    // Journal d'audit (FR-020), du plus recent au plus ancien
    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    PagedModel<AuditLogResponse> audit(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_AUDIT_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "id"));
        return new PagedModel<>(audit.list(pageable).map(AuditLogResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    CustomerResponse get(@PathVariable Long id) {
        return CustomerResponse.from(service.get(id));
    }

    // ?closureRequested=true : seulement les demandes de fermeture de compte en attente
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<CustomerResponse> list(@RequestParam(required = false) Boolean closureRequested) {
        List<Customer> customers = Boolean.TRUE.equals(closureRequested)
                ? service.listClosureRequested()
                : service.list();
        return customers.stream().map(CustomerResponse::from).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}