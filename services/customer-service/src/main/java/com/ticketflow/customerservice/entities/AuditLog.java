package com.ticketflow.customerservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 100)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(nullable = false, length = 50)
    private String resourceType;

    @Column(length = 100)
    private String resourceId;

    @Column(length = 100)
    private String correlationId;

    protected AuditLog() {
        // requis par JPA
    }

    public AuditLog(String actor, AuditAction action, String resourceType,
                    String resourceId, String correlationId) {
        this.occurredAt = Instant.now();
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.correlationId = correlationId;
    }

    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActor() { return actor; }
    public AuditAction getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public String getCorrelationId() { return correlationId; }
}