package com.ticketflow.paymentservice.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketflow.paymentservice.entities.Payment;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

class PaymentEventTest {

    private static final String SUCCEEDED_TOPIC = "ticketflow.payment.succeeded";
    private static final String FAILED_TOPIC = "ticketflow.payment.failed";

    private final JsonMapper json = JsonMapper.builder().build();

    private Payment payment() {
        Payment p = new Payment(7L, "client-1", new BigDecimal("150.00"), "MAD");
        ReflectionTestUtils.setField(p, "id", 5L);
        return p;
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> kafka() {
        return mock(KafkaTemplate.class);
    }

    private KafkaPaymentEventPublisher publisher(KafkaTemplate<String, String> kafka) {
        return new KafkaPaymentEventPublisher(kafka, json, SUCCEEDED_TOPIC, FAILED_TOPIC);
    }

    @Test
    void succeededPaymentProducesPaymentSucceeded() {
        Payment p = payment();
        p.succeed();

        PaymentEvent e = PaymentEvent.of(p);

        assertThat(e.eventType()).isEqualTo("PaymentSucceeded");
        assertThat(e.version()).isEqualTo(1);
        assertThat(e.eventId()).isNotBlank();
        assertThat(e.aggregateId()).isEqualTo("5");
        assertThat(e.correlationId()).isEqualTo("booking-7");
        assertThat(e.data().paymentId()).isEqualTo(5L);
        assertThat(e.data().bookingId()).isEqualTo(7L);
        assertThat(e.data().customerRef()).isEqualTo("client-1");
        assertThat(e.data().reason()).isNull();
    }

    @Test
    void failedAndCancelledProducePaymentFailedWithReason() {
        Payment failed = payment();
        failed.fail();
        Payment cancelled = payment();
        cancelled.cancel();

        assertThat(PaymentEvent.of(failed).eventType()).isEqualTo("PaymentFailed");
        assertThat(PaymentEvent.of(failed).data().reason()).isEqualTo("FAILURE");
        assertThat(PaymentEvent.of(cancelled).eventType()).isEqualTo("PaymentFailed");
        assertThat(PaymentEvent.of(cancelled).data().reason()).isEqualTo("CANCELLED");
    }

    @Test
    void pendingPaymentProducesNoEvent() {
        assertThatThrownBy(() -> PaymentEvent.of(payment()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void successIsPublishedAsJsonOnTheSucceededTopicKeyedByBookingId() {
        KafkaTemplate<String, String> kafka = kafka();
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        Payment p = payment();
        p.succeed();

        publisher(kafka).onPaymentEvent(PaymentEvent.of(p));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(SUCCEEDED_TOPIC), eq("7"), payload.capture());
        assertThat(payload.getValue())
                .contains("\"eventType\":\"PaymentSucceeded\"")
                .contains("\"bookingId\":7")
                .contains("\"customerRef\":\"client-1\"")
                .contains("\"data\":{");
    }

    @Test
    void failureIsPublishedOnTheFailedTopic() {
        KafkaTemplate<String, String> kafka = kafka();
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        Payment p = payment();
        p.fail();

        publisher(kafka).onPaymentEvent(PaymentEvent.of(p));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(FAILED_TOPIC), eq("7"), payload.capture());
        assertThat(payload.getValue())
                .contains("\"eventType\":\"PaymentFailed\"")
                .contains("\"reason\":\"FAILURE\"");
    }

    @Test
    void publisherSurvivesKafkaFailure() {
        KafkaTemplate<String, String> kafka = kafka();
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("broker arrete"));
        Payment p = payment();
        p.succeed();
        PaymentEvent e = PaymentEvent.of(p);

        assertThatCode(() -> publisher(kafka).onPaymentEvent(e)).doesNotThrowAnyException();
    }
}