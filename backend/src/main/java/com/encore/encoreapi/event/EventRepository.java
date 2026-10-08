package com.encore.encoreapi.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    @Query(value = "select distinct city from events where city is not null and btrim(city) <> '' order by city asc",
            nativeQuery = true)
    List<String> findDistinctCities();

    @Query(value = """
            select e.* from events e
            where (cast(:city as text) is null or lower(coalesce(e.city, '')) = lower(cast(:city as text)))
              and (
                cast(:search as text) is null
                or lower(e.name) like concat('%', lower(cast(:search as text)), '%')
                or lower(coalesce(e.artist, '')) like concat('%', lower(cast(:search as text)), '%')
                or lower(coalesce(e.venue, '')) like concat('%', lower(cast(:search as text)), '%')
                or lower(coalesce(e.city, '')) like concat('%', lower(cast(:search as text)), '%')
              )
            order by e.event_date asc nulls last, e.name asc
            """, nativeQuery = true)
    List<Event> searchCatalog(@Param("city") String city, @Param("search") String search);
}