package com.ticketflow.notification_service.adapter;

import static com.ticketflow.notification_service.TestFixtures.pendingNotification;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoggingEmailSenderTest {

    @Test
    void reportsLogProviderInsteadOfPretendingToSendEmail() {
        assertThat(new LoggingEmailSender().send(pendingNotification())).isEqualTo("log");
    }

    @Test
    void masksEmailInLogs() {
        assertThat(LoggingEmailSender.maskEmail("alice@example.com")).isEqualTo("a***@example.com");
        assertThat(LoggingEmailSender.maskEmail("bad")).isEqualTo("***");
    }
}
