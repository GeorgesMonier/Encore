package com.encore.encoreapi.event;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}