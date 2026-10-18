package com.ticketflow.event_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(nullable = false)
    private Instant eventDate;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Event() {
        // required by JPA
    }

    public Event(String title, String summary, Instant eventDate, String city,
                 String venue, String category, BigDecimal price, int capacity) {
        this.title = title;
        this.summary = summary;
        this.eventDate = eventDate;
        this.city = city;
        this.venue = venue;
        this.category = category;
        this.price = price;
        this.capacity = capacity;
    }

    public void update(String title, String summary, Instant eventDate, String city,
                       String venue, String category, BigDecimal price, int capacity) {
        this.title = title;
        this.summary = summary;
        this.eventDate = eventDate;
        this.city = city;
        this.venue = venue;
        this.category = category;
        this.price = price;
        this.capacity = capacity;
    }

    public void publish() { this.status = EventStatus.PUBLISHED; }
    public void close() { this.status = EventStatus.CLOSED; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public Instant getEventDate() { return eventDate; }
    public String getCity() { return city; }
    public String getVenue() { return venue; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public int getCapacity() { return capacity; }
    public EventStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}