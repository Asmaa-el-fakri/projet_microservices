package com.ticketflow.notification_service.service;

import static com.ticketflow.notification_service.TestFixtures.CLOCK;
import static com.ticketflow.notification_service.TestFixtures.envelope;
import static com.ticketflow.notification_service.TestFixtures.payload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.ticketflow.notification_service.TestFixtures;
import com.ticketflow.notification_service.dto.EventEnvelope;
import com.ticketflow.notification_service.entities.Notification;
import com.ticketflow.notification_service.entities.NotificationChannel;
import com.ticketflow.notification_service.entities.NotificationStatus;
import com.ticketflow.notification_service.exceptions.InvalidEventException;
import com.ticketflow.notification_service.repos.NotificationRepository;

class NotificationIngestServiceTest {

    private NotificationRepository repository;
    private PreferenceService preferences;
    private NotificationIngestService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        preferences = mock(PreferenceService.class);
        when(preferences.isAllowed(any(), any(), any())).thenReturn(true);
        service = new NotificationIngestService(new EventValidator(), new NotificationTemplateService(),
                new RecipientResolver(), preferences, repository, TestFixtures.properties(3), CLOCK);
    }

    @Test
    void createsPendingNotificationForBookingConfirmed() {
        EventEnvelope e = envelope("BookingConfirmed",
                payload("recipientEmail", "Alice@Example.com", "eventTitle", "Concert", "bookingId", "B-1"));

        List<UUID> ids = service.handle(e);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(captor.capture());
        Notification n = captor.getValue();
        assertThat(ids).containsExactly(n.getId());
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getRecipient()).isEqualTo("alice@example.com");
        assertThat(n.getEventId()).isEqualTo(e.eventId());
        assertThat(n.getEventType()).isEqualTo("BookingConfirmed");
        assertThat(n.getCorrelationId()).isEqualTo("corr-1");
        assertThat(n.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(n.getSubject()).contains("Concert");
        assertThat(n.getMessage()).contains("B-1");
        assertThat(n.getAttemptCount()).isZero();
        assertThat(n.getNextAttemptAt()).isAfter(TestFixtures.NOW);
    }

    @Test
    void duplicateEventDoesNotCreateSecondNotification() {
        EventEnvelope e = envelope("PaymentSucceeded", payload("recipientEmail", "a@b.co"));
        when(repository.existsByEventIdAndChannelAndRecipient(e.eventId(), NotificationChannel.EMAIL, "a@b.co"))
                .thenReturn(true);

        assertThat(service.handle(e)).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentDuplicateIsIgnored() {
        EventEnvelope e = envelope("PaymentFailed", payload("recipientEmail", "a@b.co"));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq"));

        assertThat(service.handle(e)).isEmpty();
    }

    @Test
    void ignoresBookingCreatedAndUnknownTypes() {
        assertThat(service.handle(envelope("BookingCreated", payload("recipientEmail", "a@b.co")))).isEmpty();
        assertThat(service.handle(envelope("SomethingElse", payload()))).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void transactionalEventWithoutRecipientIsInvalid() {
        assertThatThrownBy(() -> service.handle(envelope("TicketIssued", payload("eventTitle", "x"))))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void invalidRecipientIsRejected() {
        assertThatThrownBy(() -> service.handle(envelope("TicketIssued", payload("recipientEmail", "not-an-email"))))
                .isInstanceOf(InvalidEventException.class);
    }

    @Test
    void broadcastEventWithoutRecipientIsSilentlySkipped() {
        assertThat(service.handle(envelope("EventPublished", payload("eventTitle", "Festival")))).isEmpty();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void broadcastEventCreatesOneNotificationPerDistinctRecipient() {
        EventEnvelope e = envelope("EventPublished", payload("eventTitle", "Festival",
                "recipientEmails", List.of("a@b.co", "A@B.CO", "c@d.fr")));

        assertThat(service.handle(e)).hasSize(2);
        verify(repository, times(2)).saveAndFlush(any());
    }

    @Test
    void optOutIsRecordedAsSkippedAndNotDelivered() {
        when(preferences.isAllowed(eq("a@b.co"), eq(SupportedEventType.EVENT_PUBLISHED), any())).thenReturn(false);
        EventEnvelope e = envelope("EventPublished", payload("recipientEmail", "a@b.co", "eventTitle", "Festival"));

        assertThat(service.handle(e)).isEmpty();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
    }

    @Test
    void transactionalNotificationsIgnorePreferences() {
        when(preferences.isAllowed(any(), any(), any())).thenReturn(false);

        assertThat(service.handle(envelope("PaymentSucceeded", payload("recipientEmail", "a@b.co")))).hasSize(1);
    }

    @Test
    void notificationRequestedNeedsSubjectAndMessage() {
        assertThatThrownBy(() -> service.handle(envelope("NotificationRequested",
                payload("recipientEmail", "a@b.co", "subject", "Rappel"))))
                .isInstanceOf(InvalidEventException.class);

        List<UUID> ids = service.handle(envelope("NotificationRequested",
                payload("recipientEmail", "a@b.co", "subject", "Rappel\r\nBcc: x@y.z", "message", "Demain à 20h")));
        assertThat(ids).hasSize(1);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSubject()).doesNotContain("\r").doesNotContain("\n");
    }
}
