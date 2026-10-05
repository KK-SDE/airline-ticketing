package com.airlineticketing.repository;

import com.airlineticketing.entity.FlightInstance;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlightInstanceRepository extends JpaRepository<FlightInstance, Long> {

    @EntityGraph(attributePaths = {"schedule", "schedule.source", "schedule.destination", "schedule.aircraft"})
    @Query("""
            select fi from FlightInstance fi
            where fi.flightDate = :flightDate
              and fi.schedule.source.code = :source
              and fi.schedule.destination.code = :destination
              and fi.departureAt > :now
            order by fi.departureAt, fi.id
            """)
    Page<FlightInstance> search(@Param("source") String source,
                                @Param("destination") String destination,
                                @Param("flightDate") LocalDate flightDate,
                                @Param("now") Instant now,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"schedule", "schedule.aircraft"})
    Optional<FlightInstance> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fi from FlightInstance fi where fi.id = :id")
    Optional<FlightInstance> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select fi.flightDate from FlightInstance fi
            where fi.schedule.id = :scheduleId and fi.flightDate between :from and :to
            """)
    List<LocalDate> findFlightDates(@Param("scheduleId") Long scheduleId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);
}
