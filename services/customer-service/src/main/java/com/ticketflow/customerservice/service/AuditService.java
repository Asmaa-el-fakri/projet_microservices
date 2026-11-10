package com.ticketflow.customerservice.service;

import com.ticketflow.customerservice.config.CorrelationIdFilter;
import com.ticketflow.customerservice.entities.AuditAction;
import com.ticketflow.customerservice.entities.AuditLog;
import com.ticketflow.customerservice.repos.AuditLogRepo;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final String SYSTEM = "system";

    private final AuditLogRepo repo;

    public AuditService(AuditLogRepo repo) {
        this.repo = repo;
    }

    // Dans la meme transaction que l'operation auditee : les deux reussissent ou echouent ensemble
    @Transactional
    public void record(AuditAction action, String resourceType, String resourceId) {
        repo.save(new AuditLog(currentActor(), action, resourceType, resourceId,
                MDC.get(CorrelationIdFilter.MDC_KEY)));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    private static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return SYSTEM;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String name = jwt.getClaimAsString("preferred_username");
            return name != null ? name : jwt.getSubject();
        }
        return auth.getName();
    }
}