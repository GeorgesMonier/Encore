package com.encore.encoreapi.ticket;

import com.encore.encoreapi.event.EventRepository;
import com.encore.encoreapi.event.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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

        eventRepository.findAll().forEach(this::ensureDemoTickets);
    }

    @Transactional
    public void ensureDemoTickets(Event event) {
        if (!demoMode || !ticketTypeRepository.findByEventId(event.getId()).isEmpty()) return;
        ticketTypeRepository.save(new TicketType(event, "General (demo)", new BigDecimal("45.00"), 50));
        ticketTypeRepository.save(new TicketType(event, "VIP (demo)", new BigDecimal("90.00"), 10));
    }
}
