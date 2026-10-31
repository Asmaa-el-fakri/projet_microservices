package com.ticketflow.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketflow.bookingservice.clients.EventClient;
import com.ticketflow.bookingservice.clients.EventView;
import com.ticketflow.bookingservice.dto.BookingRequest;
import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.repos.BookingRepo;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepo repo;

    @Mock
    EventClient eventClient;

    @InjectMocks
    BookingService service;

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "/api/events/1", Map.of(),
                new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
    }

    private static EventView event() {
        return new EventView(1L, "Concert Rock", "Résumé", Instant.parse("2027-06-15T20:00:00Z"),
                "Paris", "Stade", "Musique", new BigDecimal("45.99"), 500, "PUBLISHED");
    }

    @Test
    void createsBookingWhenEventExists() {
        when(eventClient.getById(1L)).thenReturn(event());
        when(repo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = service.create(new BookingRequest(1L, 1L, 2));

        assertThat(booking.getEventId()).isEqualTo(1L);
        assertThat(booking.getQuantity()).isEqualTo(2);
    }

    @Test
    void returns404WhenEventIsUnknown() {
        when(eventClient.getById(999L)).thenThrow(
                new FeignException.NotFound("not found", request(), new byte[0], Map.of()));

        assertThatThrownBy(() -> service.create(new BookingRequest(1L, 999L, 1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(repo, never()).save(any());
    }

    @Test
    void returns503WhenEventServiceFails() {
        when(eventClient.getById(1L)).thenThrow(
                new FeignException.ServiceUnavailable("down", request(), new byte[0], Map.of()));

        assertThatThrownBy(() -> service.create(new BookingRequest(1L, 1L, 1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
        verify(repo, never()).save(any());
    }
}