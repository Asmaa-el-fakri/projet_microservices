package com.ticketflow.paymentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketflow.paymentservice.dto.PaymentRequest;
import com.ticketflow.paymentservice.dto.SimulationRequest.Outcome;
import com.ticketflow.paymentservice.entities.Payment;
import com.ticketflow.paymentservice.entities.PaymentStatus;
import com.ticketflow.paymentservice.exceptions.PaymentRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class PaymentServiceTest {

    @Autowired
    PaymentService service;

    private PaymentRequest request(long bookingId) {
        return new PaymentRequest(bookingId, new BigDecimal("150.00"), "MAD");
    }

    @Test
    void newPaymentIsPendingThenSucceeds() {
        Payment payment = service.create(request(1L), "client-1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        Payment done = service.simulate(payment.getId(), "client-1", Outcome.SUCCESS);
        assertThat(done.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void finalStateCannotChange() {
        Long id = service.create(request(2L), "client-1").getId();
        service.simulate(id, "client-1", Outcome.FAILURE);

        assertThatThrownBy(() -> service.simulate(id, "client-1", Outcome.SUCCESS))
                .isInstanceOf(PaymentRuleException.class);
    }

    @Test
    void anotherClientCannotSeeOrChangeIt() {
        Long id = service.create(request(3L), "client-1").getId();

        assertThatThrownBy(() -> service.simulate(id, "client-2", Outcome.SUCCESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.get(id, "client-2"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void onlyOneActivePaymentPerBooking() {
        service.create(request(4L), "client-1");

        assertThatThrownBy(() -> service.create(request(4L), "client-1"))
                .isInstanceOf(PaymentRuleException.class);
    }

    @Test
    void failedPaymentCanBeRetried() {
        Long id = service.create(request(5L), "client-1").getId();
        service.simulate(id, "client-1", Outcome.FAILURE);

        Payment retry = service.create(request(5L), "client-1");
        assertThat(retry.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}