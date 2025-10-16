package com.example.esame.repository;

import com.example.esame.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Integer> {

    List<Trip> findByOriginContainingIgnoreCase(String origin);
    List<Trip> findByDestinationContainingIgnoreCase(String destination);

    @Query("""
    SELECT t FROM Trip t
    WHERE (:origin IS NULL OR LOWER(t.origin) LIKE LOWER(CONCAT('%', :origin, '%')))
      AND (:destination IS NULL OR LOWER(t.destination) LIKE LOWER(CONCAT('%', :destination, '%')))
  """)
    List<Trip> searchTrips(@Param("origin") String origin, @Param("destination") String destination);
}
