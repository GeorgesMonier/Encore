package com.encore.encoreapi.event;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final EventSyncService eventSyncService;

    public EventController(EventRepository eventRepository, EventSyncService eventSyncService) {
        this.eventRepository = eventRepository;
        this.eventSyncService = eventSyncService;
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search) {
        String normalizedCity = city == null || city.isBlank() ? null : city.strip();
        String normalizedSearch = search == null || search.isBlank() ? null : search.strip();
        return ResponseEntity.ok(eventRepository.searchCatalog(normalizedCity, normalizedSearch));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAvailableCities() {
        return ResponseEntity.ok(eventRepository.findDistinctCities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable UUID id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncEvents(
            @RequestParam(required = false, defaultValue = "Madrid") String city,
            @RequestParam(required = false, defaultValue = "ES") String countryCode) {
        int count = eventSyncService.syncEvents(city, countryCode);
        return ResponseEntity.ok("Se sincronizaron " + count + " eventos nuevos.");
    }
}