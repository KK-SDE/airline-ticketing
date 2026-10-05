package com.airlineticketing.repository;

import com.airlineticketing.dto.FlightSeatCount;
import com.airlineticketing.entity.BookingSeat;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(UUID bookingId);

    @Query("select bs.seatLabel from BookingSeat bs where bs.flightInstance.id = :flightId and bs.active = true")
    List<String> findActiveSeatLabels(@Param("flightId") Long flightId);

    @Query("""
            select bs.seatLabel from BookingSeat bs
            where bs.flightInstance.id = :flightId and bs.active = true and bs.seatLabel in :labels
            """)
    List<String> findActiveSeatLabelsIn(@Param("flightId") Long flightId, @Param("labels") Collection<String> labels);

    @Query("""
            select new com.airlineticketing.dto.FlightSeatCount(bs.flightInstance.id, count(bs))
            from BookingSeat bs
            where bs.flightInstance.id in :flightIds and bs.active = true
            group by bs.flightInstance.id
            """)
    List<FlightSeatCount> countActiveByFlightIds(@Param("flightIds") Collection<Long> flightIds);
}
