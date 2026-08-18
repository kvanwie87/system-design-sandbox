package com.example.inventory.service;

import com.example.common.dto.InventoryDTO;
import com.example.common.enums.ReservationStatus;
import com.example.common.request.ReserveInventoryRequest;
import com.example.common.response.ReservationResponse;
import com.example.common.util.IdGenerator;
import com.example.inventory.entity.Inventory;
import com.example.inventory.entity.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final int MAX_RETRIES = 3;

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final Duration reservationTtl;

    public InventoryService(InventoryRepository inventoryRepository,
                            ReservationRepository reservationRepository,
                            @Value("${inventory.reservation-ttl-minutes:10}") int ttlMinutes) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.reservationTtl = Duration.ofMinutes(ttlMinutes);
    }

    public InventoryDTO checkAvailability(String productId) {
        int totalAvailable = inventoryRepository.getTotalAvailableQty(productId);
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);

        int totalReserved = inventories.stream().mapToInt(Inventory::getReservedQty).sum();

        return new InventoryDTO(
                null,
                productId,
                "ALL",
                totalAvailable,
                totalReserved,
                Instant.now().toString()
        );
    }

    @Transactional
    public ReservationResponse reserveInventory(ReserveInventoryRequest request) {
        return reserveWithRetry(request, 0);
    }

    private ReservationResponse reserveWithRetry(ReserveInventoryRequest request, int attempt) {
        try {
            List<Inventory> inventories = inventoryRepository.findByProductId(request.productId());
            if (inventories.isEmpty()) {
                throw new IllegalStateException("No inventory found for product: " + request.productId());
            }

            int remaining = request.quantity();
            // Try to reserve from available inventory across warehouses
            for (Inventory inv : inventories) {
                if (remaining <= 0) break;
                int canReserve = Math.min(remaining, inv.getAvailableQty());
                if (canReserve > 0) {
                    inv.setAvailableQty(inv.getAvailableQty() - canReserve);
                    inv.setReservedQty(inv.getReservedQty() + canReserve);
                    inventoryRepository.save(inv);
                    remaining -= canReserve;
                }
            }

            if (remaining > 0) {
                throw new IllegalStateException("Insufficient inventory for product: " + request.productId()
                        + ". Requested: " + request.quantity() + ", available: " + (request.quantity() - remaining));
            }

            // Create reservation record
            Reservation reservation = new Reservation();
            reservation.setId(IdGenerator.generate("res"));
            reservation.setProductId(request.productId());
            reservation.setQuantity(request.quantity());
            reservation.setStatus(ReservationStatus.ACTIVE);
            reservation.setExpiresAt(Instant.now().plus(reservationTtl));
            reservation.setOrderId(request.orderId());

            reservationRepository.save(reservation);

            return new ReservationResponse(
                    reservation.getId(),
                    reservation.getProductId(),
                    reservation.getQuantity(),
                    reservation.getStatus(),
                    reservation.getExpiresAt().toString()
            );
        } catch (ObjectOptimisticLockingFailureException e) {
            if (attempt < MAX_RETRIES) {
                log.warn("Optimistic lock conflict for product {}, retrying (attempt {})",
                        request.productId(), attempt + 1);
                return reserveWithRetry(request, attempt + 1);
            }
            throw new IllegalStateException("Could not reserve inventory due to concurrent modification. Please retry.");
        }
    }

    @Transactional
    public void confirmReservation(String orderId) {
        List<Reservation> reservations = reservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);

        for (Reservation reservation : reservations) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);

            // Confirmed means the reserved qty is now permanently deducted; reduce reservedQty
            List<Inventory> inventories = inventoryRepository.findByProductId(reservation.getProductId());
            int remaining = reservation.getQuantity();
            for (Inventory inv : inventories) {
                if (remaining <= 0) break;
                int canConfirm = Math.min(remaining, inv.getReservedQty());
                if (canConfirm > 0) {
                    inv.setReservedQty(inv.getReservedQty() - canConfirm);
                    inventoryRepository.save(inv);
                    remaining -= canConfirm;
                }
            }
        }

        log.info("Confirmed reservations for order: {}", orderId);
    }

    @Transactional
    public void releaseReservation(String orderId) {
        List<Reservation> reservations = reservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);

        for (Reservation reservation : reservations) {
            releaseReservationStock(reservation);
        }

        log.info("Released reservations for order: {}", orderId);
    }

    @Transactional
    public void expireStaleReservations() {
        List<Reservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, Instant.now());

        for (Reservation reservation : expired) {
            releaseReservationStock(reservation);
            log.info("Expired reservation {} for product {} (order {})",
                    reservation.getId(), reservation.getProductId(), reservation.getOrderId());
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} stale reservations", expired.size());
        }
    }

    private void releaseReservationStock(Reservation reservation) {
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        // Return stock to available
        List<Inventory> inventories = inventoryRepository.findByProductId(reservation.getProductId());
        int remaining = reservation.getQuantity();
        for (Inventory inv : inventories) {
            if (remaining <= 0) break;
            int canRelease = Math.min(remaining, inv.getReservedQty());
            if (canRelease > 0) {
                inv.setAvailableQty(inv.getAvailableQty() + canRelease);
                inv.setReservedQty(inv.getReservedQty() - canRelease);
                inventoryRepository.save(inv);
                remaining -= canRelease;
            }
        }
    }
}
