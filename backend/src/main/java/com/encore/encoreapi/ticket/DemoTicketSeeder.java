package com.encore.encoreapi.ticket;

import com.encore.encoreapi.event.EventRepository;
import com.encore.encoreapi.event.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DemoTicketSeeder implements ApplicationRunner {

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final boolean demoMode;

    public DemoTicketSeeder(EventRepository eventRepository,
                            TicketTypeRepository ticketTypeRepository,
                            @Value("${payments.demo-mode:true}") boolean demoMode) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.demoMode = demoMode;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoMode) return;

        if (eventRepository.count() == 0) {
            seedSampleEvents();
        }
        eventRepository.findAll().forEach(this::ensureDemoTickets);
    }

    @Transactional
    public void ensureDemoTickets(Event event) {
        if (!demoMode || !ticketTypeRepository.findByEventId(event.getId()).isEmpty()) return;
        ticketTypeRepository.save(new TicketType(event, "General (demo)", new BigDecimal("45.00"), 50));
        ticketTypeRepository.save(new TicketType(event, "VIP (demo)", new BigDecimal("90.00"), 10));
    }

    private void seedSampleEvents() {
        LocalDateTime now = LocalDateTime.now();
        eventRepository.saveAll(List.of(
                new Event("encore-demo-live-1", "Encore Demo Live: Noche Eléctrica", "The Demo Waves",
                        "Sala Demo Encore", "Barcelona", now.plusDays(30), null,
                        "Evento ficticio de portfolio. Los precios y el inventario son de demostración."),
                new Event("encore-demo-live-2", "Encore Demo Live: Sesión Acústica", "Luna de Prueba",
                        "Auditorio Demo", "Madrid", now.plusDays(45), null,
                        "Evento ficticio de portfolio. No se realizan cargos reales."),
                new Event("encore-demo-live-3", "Encore Demo Live: Festival Urbano", "Banda Ficticia",
                        "Parque Demo", "Valencia", now.plusDays(60), null,
                        "Evento ficticio de portfolio. No se realizan cargos reales.")
        ));
    }
}
