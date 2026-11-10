package com.ticketflow.customerservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketflow.customerservice.entities.AuditAction;
import com.ticketflow.customerservice.entities.AuditLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuditServiceTest {

    @Autowired
    AuditService audit;

    @AfterEach
    void clean() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    private AuditLog latest() {
        return audit.list(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id")))
                .getContent().get(0);
    }

    @Test
    void recordsActorActionResourceAndCorrelation() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("admin1", "n/a"));
        MDC.put("correlationId", "req-123");

        audit.record(AuditAction.CUSTOMER_DELETED, "Customer", "42");

        AuditLog entry = latest();
        assertThat(entry.getActor()).isEqualTo("admin1");
        assertThat(entry.getAction()).isEqualTo(AuditAction.CUSTOMER_DELETED);
        assertThat(entry.getResourceType()).isEqualTo("Customer");
        assertThat(entry.getResourceId()).isEqualTo("42");
        assertThat(entry.getCorrelationId()).isEqualTo("req-123");
        assertThat(entry.getOccurredAt()).isNotNull();
    }

    @Test
    void actorIsSystemWithoutAuthentication() {
        audit.record(AuditAction.CUSTOMER_CREATED, "Customer", "7");

        assertThat(latest().getActor()).isEqualTo("system");
    }

    @Test
    void actorIsPreferredUsernameOfTheToken() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject("sub-1").claim("preferred_username", "organizer1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        audit.record(AuditAction.CLOSURE_REQUESTED, "Customer", "9");

        assertThat(latest().getActor()).isEqualTo("organizer1");
    }

    @Test
    void actorFallsBackToSubjectWhenNoUsername() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("sub-2").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        audit.record(AuditAction.CUSTOMER_UPDATED, "Customer", "3");

        assertThat(latest().getActor()).isEqualTo("sub-2");
    }
}