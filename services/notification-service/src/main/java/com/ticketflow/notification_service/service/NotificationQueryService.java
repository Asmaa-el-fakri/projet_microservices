package com.ticketflow.notification_service.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationStatus;
import com.ticketflow.notification_service.exceptions.NotificationNotFoundException;
import com.ticketflow.notification_service.repos.NotificationRepository;

@Service
public class NotificationQueryService {

    static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository repository;

    public NotificationQueryService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Page<Notification> findForRecipient(String email, int page, int size) {
        return repository.findByRecipient(email.trim().toLowerCase(), pageable(page, size));
    }

    public Page<Notification> findAll(NotificationStatus status, int page, int size) {
        return status == null
                ? repository.findAll(pageable(page, size))
                : repository.findByStatus(status, pageable(page, size));
    }

    public Notification get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    static PageRequest pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
