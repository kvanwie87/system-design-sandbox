package com.example.inventory.repository;

import com.example.common.enums.ReservationStatus;
import com.example.inventory.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

    Optional<Reservation> findByOrderIdAndProductIdAndStatus(String orderId, String productId, ReservationStatus status);

    List<Reservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
}
