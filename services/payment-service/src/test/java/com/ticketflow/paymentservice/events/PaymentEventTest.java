package com.ticketflow.paymentservice.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketflow.paymentservice.entities.Payment;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentEventTest {

    private Payment payment() {
        Payment p = new Payment(7L, "client-1", new BigDecimal("150.00"), "MAD");
        ReflectionTestUtils.setField(p, "id", 5L);
        return p;
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, Object> kafka() {
        return mock(KafkaTemplate.class);
    }

    @Test
    void succeededPaymentProducesPaymentSucceeded() {
        Payment p = payment();
        p.succeed();

        PaymentEvent e = PaymentEvent.of(p);

        assertThat(e.type()).isEqualTo("PaymentSucceeded");
        assertThat(e.version()).isEqualTo(1);
        assertThat(e.eventId()).isNotBlank();
        assertThat(e.paymentId()).isEqualTo(5L);
        assertThat(e.bookingId()).isEqualTo(7L);
        assertThat(e.correlationId()).isEqualTo("booking-7");
        assertThat(e.reason()).isNull();
    }

    @Test
    void failedAndCancelledProducePaymentFailedWithReason() {
        Payment failed = payment();
        failed.fail();
        Payment cancelled = payment();
        cancelled.cancel();

        assertThat(PaymentEvent.of(failed).type()).isEqualTo("PaymentFailed");
        assertThat(PaymentEvent.of(failed).reason()).isEqualTo("FAILURE");
        assertThat(PaymentEvent.of(cancelled).type()).isEqualTo("PaymentFailed");
        assertThat(PaymentEvent.of(cancelled).reason()).isEqualTo("CANCELLED");
    }

    @Test
    void pendingPaymentProducesNoEvent() {
        assertThatThrownBy(() -> PaymentEvent.of(payment()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publisherSendsEventKeyedByBookingId() {
        KafkaTemplate<String, Object> kafka = kafka();
        when(kafka.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(kafka, "payments.events");
        Payment p = payment();
        p.succeed();
        PaymentEvent e = PaymentEvent.of(p);

        publisher.onPaymentEvent(e);

        verify(kafka).send("payments.events", "7", e);
    }

    @Test
    void publisherSurvivesKafkaFailure() {
        KafkaTemplate<String, Object> kafka = kafka();
        when(kafka.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("broker arrete"));
        KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(kafka, "payments.events");
        Payment p = payment();
        p.succeed();
        PaymentEvent e = PaymentEvent.of(p);

        assertThatCode(() -> publisher.onPaymentEvent(e)).doesNotThrowAnyException();
    }
}