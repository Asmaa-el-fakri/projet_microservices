package com.ticketflow.notification_service.service;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ticketflow.notification_service.config.NotificationProperties;
import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationStatus;
import com.ticketflow.notification_service.repos.NotificationRepository;

/** Relance les notifications PENDING dont la prochaine tentative est échue (reprises et reprise après crash). */
@Component
public class NotificationRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryScheduler.class);

    private final NotificationRepository repository;
    private final NotificationDeliveryService delivery;
    private final NotificationProperties properties;
    private final Clock clock;

    public NotificationRetryScheduler(NotificationRepository repository, NotificationDeliveryService delivery,
                                      NotificationProperties properties, Clock clock) {
        this.repository = repository;
        this.delivery = delivery;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${notification.delivery.retry-scan-interval-ms:15000}")
    public void retryDue() {
        List<Notification> due = repository.findDue(NotificationStatus.PENDING, clock.instant(),
                PageRequest.of(0, properties.delivery().retryBatchSize()));
        if (!due.isEmpty()) {
            log.info("{} notification(s) à relancer", due.size());
        }
        due.forEach(n -> delivery.deliver(n.getId()));
    }
}
