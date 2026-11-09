package com.ticketflow.paymentservice.controllers;

import com.ticketflow.paymentservice.dto.PaymentRequest;
import com.ticketflow.paymentservice.dto.PaymentResponse;
import com.ticketflow.paymentservice.dto.SimulationRequest;
import com.ticketflow.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        PaymentResponse created = PaymentResponse.from(service.create(request, jwt.getSubject()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    PaymentResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return PaymentResponse.from(service.get(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/simulate")
    @PreAuthorize("hasRole('CLIENT')")
    PaymentResponse simulate(@PathVariable Long id,
                             @Valid @RequestBody SimulationRequest request,
                             @AuthenticationPrincipal Jwt jwt) {
        return PaymentResponse.from(service.simulate(id, jwt.getSubject(), request.outcome()));
    }
}