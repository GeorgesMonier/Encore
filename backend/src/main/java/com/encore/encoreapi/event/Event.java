package com.encore.encoreapi.event;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    private String artist;

    private String venue;

    private String city;

    private LocalDateTime eventDate;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 2000)
    private String description;

    public Event() {}

    public Event(String externalId, String name, String artist, String venue,
                 String city, LocalDateTime eventDate, String imageUrl, String description) {
        this.externalId = externalId;
        this.name = name;
        this.artist = artist;
        this.venue = venue;
        this.city = city;
        this.eventDate = eventDate;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getVenue() { return venue; }
    public String getCity() { return city; }
    public LocalDateTime getEventDate() { return eventDate; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
}