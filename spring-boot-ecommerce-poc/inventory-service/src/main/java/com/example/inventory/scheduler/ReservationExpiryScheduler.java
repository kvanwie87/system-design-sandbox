package com.example.inventory.scheduler;

import com.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final InventoryService inventoryService;

    public ReservationExpiryScheduler(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void expireReservations() {
        log.debug("Running reservation expiry check...");
        inventoryService.expireStaleReservations();
    }
}
