package com.ticketflow.paymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketflow.paymentservice.dto.SimulationRequest.Outcome;
import com.ticketflow.paymentservice.entities.Payment;
import com.ticketflow.paymentservice.events.PaymentEvent;
import com.ticketflow.paymentservice.repos.PaymentRepo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@RecordApplicationEvents
@Transactional
class PaymentServiceEventsTest {

    private static final String OWNER = "client-1";

    @Autowired
    PaymentService service;

    @Autowired
    PaymentRepo repo;

    @Autowired
    ApplicationEvents events;

    private Long newPaymentId(long bookingId) {
        return repo.save(new Payment(bookingId, OWNER, new BigDecimal("100.00"), "MAD")).getId();
    }

    private List<PaymentEvent> published() {
        return events.stream(PaymentEvent.class).toList();
    }

    @Test
    void successPublishesPaymentSucceeded() {
        Long id = newPaymentId(701L);

        service.simulate(id, OWNER, Outcome.SUCCESS);

        assertThat(published()).hasSize(1);
        assertThat(published().get(0).eventType()).isEqualTo("PaymentSucceeded");
        assertThat(published().get(0).data().bookingId()).isEqualTo(701L);
    }

    @Test
    void failurePublishesPaymentFailed() {
        Long id = newPaymentId(702L);

        service.simulate(id, OWNER, Outcome.FAILURE);

        assertThat(published()).hasSize(1);
        assertThat(published().get(0).eventType()).isEqualTo("PaymentFailed");
        assertThat(published().get(0).data().reason()).isEqualTo("FAILURE");
    }

    @Test
    void cancelPublishesPaymentFailedWithCancelledReason() {
        Long id = newPaymentId(703L);

        service.simulate(id, OWNER, Outcome.CANCEL);

        assertThat(published()).hasSize(1);
        assertThat(published().get(0).data().reason()).isEqualTo("CANCELLED");
    }

    @Test
    void anotherCustomerGets404AndNoEventIsPublished() {
        Long id = newPaymentId(704L);

        assertThatThrownBy(() -> service.simulate(id, "autre-client", Outcome.SUCCESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(published()).isEmpty();
    }
}