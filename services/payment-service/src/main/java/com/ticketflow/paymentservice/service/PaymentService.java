package com.ticketflow.paymentservice.service;

import com.ticketflow.paymentservice.dto.PaymentRequest;
import com.ticketflow.paymentservice.dto.SimulationRequest.Outcome;
import com.ticketflow.paymentservice.entities.Payment;
import com.ticketflow.paymentservice.entities.PaymentStatus;
import com.ticketflow.paymentservice.events.PaymentEvent;
import com.ticketflow.paymentservice.exceptions.PaymentRuleException;
import com.ticketflow.paymentservice.repos.PaymentRepo;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private static final String DEFAULT_CURRENCY = "MAD";

    private final PaymentRepo repo;
    private final ApplicationEventPublisher events;
    private final boolean simulationEnabled;

    public PaymentService(PaymentRepo repo,
                          ApplicationEventPublisher events,
                          @Value("${ticketflow.payments.simulation-enabled:true}") boolean simulationEnabled) {
        this.repo = repo;
        this.events = events;
        this.simulationEnabled = simulationEnabled;
    }

    @Transactional
    public Payment create(PaymentRequest request, String ownerId) {
        // Un seul paiement actif (en attente ou reussi) par reservation
        if (repo.existsByBookingIdAndStatusIn(request.bookingId(),
                List.of(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED))) {
            throw new PaymentRuleException(
                    "Un paiement est deja en cours ou reussi pour la reservation " + request.bookingId());
        }
        String currency = request.currency() == null ? DEFAULT_CURRENCY : request.currency();
        return repo.save(new Payment(request.bookingId(), ownerId, request.amount(), currency));
    }

    @Transactional(readOnly = true)
    public Payment get(Long id, String ownerId) {
        return findOwned(id, ownerId);
    }

    @Transactional
    public Payment simulate(Long id, String ownerId, Outcome outcome) {
        if (!simulationEnabled) {
            throw notFound();
        }
        Payment payment = findOwned(id, ownerId);
        switch (outcome) {
            case SUCCESS -> payment.succeed();
            case FAILURE -> payment.fail();
            case CANCEL -> payment.cancel();
        }
        // Publie apres le commit par KafkaPaymentEventPublisher
        events.publishEvent(PaymentEvent.of(payment));
        return payment;
    }

    // Le paiement d'un autre client repond 404 : on ne revele pas son existence
    private Payment findOwned(Long id, String ownerId) {
        return repo.findByIdAndOwnerId(id, ownerId).orElseThrow(PaymentService::notFound);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
    }
}