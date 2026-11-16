package com.ticketflow.notification_service.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ticketflow.notification_service.dto.NotificationResponse;
import com.ticketflow.notification_service.dto.PageResponse;
import com.ticketflow.notification_service.entities.NotificationStatus;
import com.ticketflow.notification_service.service.NotificationQueryService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationQueryService queryService;

    public NotificationController(NotificationQueryService queryService) {
        this.queryService = queryService;
    }

    /** Historique des notifications de l'utilisateur connecté (identité issue du jeton, jamais d'un paramètre). */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT','ORGANIZER','ADMIN')")
    public PageResponse<NotificationResponse> myNotifications(@AuthenticationPrincipal Jwt jwt,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Jeton sans adresse email");
        }
        return PageResponse.of(queryService.findForRecipient(email, page, size), NotificationResponse::forOwner);
    }

    /** Supervision : liste filtrable par statut. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<NotificationResponse> list(@RequestParam(required = false) NotificationStatus status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(queryService.findAll(status, page, size), NotificationResponse::forAdmin);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NotificationResponse get(@PathVariable UUID id) {
        return NotificationResponse.forAdmin(queryService.get(id));
    }
}
