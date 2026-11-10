package com.ticketflow.customerservice.dto;

import com.ticketflow.customerservice.entities.AuditAction;
import com.ticketflow.customerservice.entities.AuditLog;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        String actor,
        AuditAction action,
        String resourceType,
        String resourceId,
        String correlationId) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getOccurredAt(), a.getActor(), a.getAction(),
                a.getResourceType(), a.getResourceId(), a.getCorrelationId());
    }
}