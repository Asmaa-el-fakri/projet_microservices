package com.ticketflow.notification_service.service;

import static com.ticketflow.notification_service.TestFixtures.CLOCK;
import static com.ticketflow.notification_service.TestFixtures.NOW;
import static com.ticketflow.notification_service.TestFixtures.pendingNotification;
import static com.ticketflow.notification_service.TestFixtures.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.ticketflow.notification_service.adapter.NotificationSender;
import com.ticketflow.notification_service.adapter.SenderRegistry;
import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;
import com.ticketflow.notification_service.entities.NotificationStatus;
import com.ticketflow.notification_service.exceptions.DeliveryException;
import com.ticketflow.notification_service.repos.NotificationRepository;

class NotificationDeliveryServiceTest {

    private NotificationRepository repository;
    private NotificationSender sender;
    private NotificationDeliveryService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        sender = mock(NotificationSender.class);
        when(sender.channel()).thenReturn(NotificationChannel.EMAIL);
        service = new NotificationDeliveryService(repository, new SenderRegistry(List.of(sender)),
                properties(3), CLOCK);
    }

    @Test
    void successfulDeliveryMarksSent() {
        Notification n = pendingNotification();
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));
        when(sender.send(n)).thenReturn("smtp");

        service.deliver(n.getId());

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getProvider()).isEqualTo("smtp");
        assertThat(n.getSentAt()).isEqualTo(NOW);
        assertThat(n.getAttemptCount()).isEqualTo(1);
        assertThat(n.getLastError()).isNull();
        verify(repository).save(n);
    }

    @Test
    void failedDeliveryStaysPendingWithBackoff() {
        Notification n = pendingNotification();
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));
        doThrow(new DeliveryException("smtp down")).when(sender).send(n);

        service.deliver(n.getId());

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getAttemptCount()).isEqualTo(1);
        assertThat(n.getLastError()).contains("smtp down");
        assertThat(n.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofSeconds(30)));
        verify(repository).save(n);
    }

    @Test
    void backoffGrowsExponentiallyThenFails() {
        Notification n = pendingNotification();
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));
        doThrow(new DeliveryException("down")).when(sender).send(n);

        service.deliver(n.getId());
        assertThat(n.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofSeconds(30)));
        service.deliver(n.getId());
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofSeconds(60)));
        service.deliver(n.getId());

        assertThat(n.getAttemptCount()).isEqualTo(3);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.getNextAttemptAt()).isNull();
    }

    @Test
    void retrySucceedsAfterEarlierFailure() {
        Notification n = pendingNotification();
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));
        when(sender.send(n)).thenThrow(new DeliveryException("down")).thenReturn("smtp");

        service.deliver(n.getId());
        service.deliver(n.getId());

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getAttemptCount()).isEqualTo(2);
        assertThat(n.getLastError()).isNull();
    }

    @Test
    void alreadyProcessedNotificationIsNotSentAgain() {
        Notification n = pendingNotification();
        n.markSent(NOW, "smtp");
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));

        service.deliver(n.getId());

        verify(sender, never()).send(any());
        verify(repository, never()).save(any());
    }

    @Test
    void unknownNotificationIsIgnored() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        service.deliver(java.util.UUID.randomUUID());

        verify(sender, never()).send(any());
    }

    @Test
    void optimisticLockConflictIsSwallowed() {
        Notification n = pendingNotification();
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));
        when(sender.send(n)).thenReturn("smtp");
        doThrow(new ObjectOptimisticLockingFailureException(Notification.class, n.getId()))
                .when(repository).save(n);

        service.deliver(n.getId()); // ne doit pas lever d'exception
    }

    @Test
    void missingAdapterCountsAsFailedAttempt() {
        NotificationDeliveryService noSender = new NotificationDeliveryService(repository,
                new SenderRegistry(List.of()), properties(3), CLOCK);
        Notification n = pendingNotification();
        when(repository.findById(n.getId())).thenReturn(Optional.of(n));

        noSender.deliver(n.getId());

        assertThat(n.getAttemptCount()).isEqualTo(1);
        assertThat(n.getLastError()).contains("Aucun adaptateur");
    }
}
