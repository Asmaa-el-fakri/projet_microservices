package com.ticketflow.customerservice.repos;

import com.ticketflow.customerservice.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
}