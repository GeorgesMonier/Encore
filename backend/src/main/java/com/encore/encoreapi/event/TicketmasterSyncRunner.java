package com.encore.encoreapi.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
public class TicketmasterSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketmasterSyncRunner.class);
    private final EventSyncService eventSyncService;
    private final boolean enabled;
    private final List<String> cities;

    public TicketmasterSyncRunner(
            EventSyncService eventSyncService,
            @Value("${ticketmaster.sync.enabled:false}") boolean enabled,
            @Value("${ticketmaster.sync.cities:Madrid,Barcelona,Valencia,Sevilla,Bilbao,Zaragoza,Málaga,Alicante,Granada}") String cities) {
        this.eventSyncService = eventSyncService;
        this.enabled = enabled;
        this.cities = Arrays.stream(cities.split(","))
                .map(String::trim)
                .filter(city -> !city.isEmpty())
                .distinct()
                .toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Sincronización automática de Ticketmaster desactivada");
            return;
        }

        for (String city : cities) {
            try {
                int imported = eventSyncService.syncEvents(city, "ES");
                log.info("Sincronización de Ticketmaster en {} completada: {} eventos nuevos", city, imported);
            } catch (RestClientException exception) {
                log.error("No se pudieron sincronizar los conciertos de Ticketmaster para {}: {}",
                        city, exception.getMessage(), exception);
            }
        }
    }
}
