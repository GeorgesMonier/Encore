package com.encore.encoreapi.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Service
public class TicketmasterClient {

    private final RestClient restClient;

    @Value("${ticketmaster.api.key}")
    private String apiKey;

    public TicketmasterClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2")
                .build();
    }

    public List<TicketmasterEventDto> searchEvents(String city, String countryCode) {
        TicketmasterSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/events.json")
                        .queryParam("apikey", apiKey)
                        .queryParam("city", city)
                        .queryParam("countryCode", countryCode)
                        .queryParam("classificationName", "music")
                        .queryParam("size", "20")
                        .build())
                .retrieve()
                .body(TicketmasterSearchResponse.class);

        if (response == null || response.get_embedded() == null) {
            return Collections.emptyList();
        }

        return response.get_embedded().getEvents();
    }
}