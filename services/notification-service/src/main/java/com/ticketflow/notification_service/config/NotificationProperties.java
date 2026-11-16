package com.ticketflow.notification_service.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        @DefaultValue Delivery delivery,
        @DefaultValue Email email,
        @DefaultValue Kafka kafka) {

    public record Delivery(
            @DefaultValue("5") int maxAttempts,
            @DefaultValue("30s") Duration retryBaseDelay,
            @DefaultValue("50") int retryBatchSize) {
    }

    public record Email(
            @DefaultValue("log") String mode,
            @DefaultValue("no-reply@ticketflow.local") String from) {
    }

    public record Kafka(
            @DefaultValue("notification-service") String groupId,
            @DefaultValue(".DLT") String dltSuffix,
            @DefaultValue("3") int consumerRetryAttempts,
            @DefaultValue("1000") long consumerRetryIntervalMs,
            List<String> topics) {
    }
}
