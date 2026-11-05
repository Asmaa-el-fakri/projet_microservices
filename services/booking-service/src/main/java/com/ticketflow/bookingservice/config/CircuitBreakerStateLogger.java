package com.ticketflow.bookingservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerStateLogger {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerStateLogger.class);

    public CircuitBreakerStateLogger(CircuitBreakerRegistry registry) {
        // Les circuits sont créés au premier appel : on s'abonne dès leur création
        registry.getEventPublisher().onEntryAdded(event -> watch(event.getAddedEntry()));
        registry.getAllCircuitBreakers().forEach(this::watch);
    }

    private void watch(CircuitBreaker cb) {
        log.info("Circuit '{}' créé, état initial : {}", cb.getName(), cb.getState());
        cb.getEventPublisher().onStateTransition(e ->
                log.warn("Circuit '{}' : changement d'état {}", e.getCircuitBreakerName(), e.getStateTransition()));
    }
}