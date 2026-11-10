package com.ticketflow.event_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketflow.event_service.entities.Event;
import com.ticketflow.event_service.repos.EventRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventSearchTest {

    @Autowired
    EventRepo repo;

    @Autowired
    EventService service;

    @BeforeEach
    void init() {
        save("Concert Rock", "Paris", "Musique", "45.99", "2027-06-15T20:00:00Z", true);
        save("Festival Jazz", "Casablanca", "Musique", "150.00", "2027-07-01T19:00:00Z", true);
        save("Match de foot", "Casablanca", "Sport", "80.00", "2027-08-10T18:00:00Z", true);
        save("Brouillon secret", "Casablanca", "Musique", "10.00", "2027-09-01T18:00:00Z", false);
    }

    private void save(String title, String city, String category, String price, String date, boolean published) {
        Event e = new Event(title, "Resume " + title, Instant.parse(date), city, "Salle",
                category, new BigDecimal(price), 100);
        if (published) {
            e.publish();
        }
        repo.save(e);
    }

    private List<String> titles(String q, String city, String category, Instant from, Instant toExcl,
                                String min, String max) {
        EventSearchCriteria c = new EventSearchCriteria(q, city, category, from, toExcl,
                min == null ? null : new BigDecimal(min), max == null ? null : new BigDecimal(max));
        return service.search(c, PageRequest.of(0, 10, Sort.by("eventDate"))).getContent()
                .stream().map(Event::getTitle).toList();
    }

    @Test
    void neverReturnsDrafts() {
        assertThat(titles(null, null, null, null, null, null, null))
                .containsExactlyInAnyOrder("Concert Rock", "Festival Jazz", "Match de foot");
    }

    @Test
    void filtersByCityAndCategoryIgnoringCase() {
        assertThat(titles(null, "casablanca", "musique", null, null, null, null))
                .containsExactly("Festival Jazz");
    }

    @Test
    void filtersByPriceRange() {
        assertThat(titles(null, null, null, null, null, null, "100"))
                .containsExactlyInAnyOrder("Concert Rock", "Match de foot");
    }

    @Test
    void searchesTextInTitle() {
        assertThat(titles("JAZZ", null, null, null, null, null, null))
                .containsExactly("Festival Jazz");
    }

    @Test
    void filtersByDatePeriod() {
        assertThat(titles(null, null, null, Instant.parse("2027-07-01T00:00:00Z"),
                Instant.parse("2027-08-01T00:00:00Z"), null, null))
                .containsExactly("Festival Jazz");
    }

    @Test
    void treatsPercentAsLiteralCharacter() {
        assertThat(titles("%", null, null, null, null, null, null)).isEmpty();
    }
}