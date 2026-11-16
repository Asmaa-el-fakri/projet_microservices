package com.ticketflow.notification_service.service;

import static com.ticketflow.notification_service.TestFixtures.CLOCK;
import static com.ticketflow.notification_service.TestFixtures.NOW;
import static com.ticketflow.notification_service.TestFixtures.pendingNotification;
import static com.ticketflow.notification_service.TestFixtures.properties;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationStatus;
import com.ticketflow.notification_service.repos.NotificationRepository;

class NotificationRetrySchedulerTest {

    @Test
    void redeliversEveryDueNotification() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationDeliveryService delivery = mock(NotificationDeliveryService.class);
        Notification a = pendingNotification();
        Notification b = pendingNotification();
        when(repository.findDue(eq(NotificationStatus.PENDING), eq(NOW), any())).thenReturn(List.of(a, b));

        new NotificationRetryScheduler(repository, delivery, properties(3), CLOCK).retryDue();

        verify(delivery).deliver(a.getId());
        verify(delivery).deliver(b.getId());
    }

    @Test
    void doesNothingWhenNothingIsDue() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationDeliveryService delivery = mock(NotificationDeliveryService.class);
        when(repository.findDue(any(), any(), any())).thenReturn(List.of());

        new NotificationRetryScheduler(repository, delivery, properties(3), CLOCK).retryDue();

        verify(delivery, never()).deliver(any());
    }
}
