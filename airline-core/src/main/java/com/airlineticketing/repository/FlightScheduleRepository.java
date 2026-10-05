package com.airlineticketing.repository;

import com.airlineticketing.entity.FlightSchedule;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {

    boolean existsByFlightNumber(String flightNumber);

    @Override
    @EntityGraph(attributePaths = {"source", "destination", "aircraft"})
    Page<FlightSchedule> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"source", "destination", "aircraft", "daysOfOperation"})
    Optional<FlightSchedule> findWithDetailsById(Long id);
}
