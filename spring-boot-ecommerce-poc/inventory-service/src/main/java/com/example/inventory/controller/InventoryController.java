package com.example.inventory.controller;

import com.example.common.dto.InventoryDTO;
import com.example.common.exception.ApiError;
import com.example.common.request.ReserveInventoryRequest;
import com.example.common.response.ReservationResponse;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}/availability")
    public ResponseEntity<InventoryDTO> checkAvailability(@PathVariable String productId) {
        InventoryDTO availability = inventoryService.checkAvailability(productId);
        return ResponseEntity.ok(availability);
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        try {
            ReservationResponse response = inventoryService.reserveInventory(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(409, "INSUFFICIENT_INVENTORY", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmReservation(@RequestBody Map<String, String> request) {
        String orderId = request.get("orderId");
        if (orderId == null || orderId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "INVALID_REQUEST", "orderId is required"));
        }
        inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/release")
    public ResponseEntity<?> releaseReservation(@RequestBody Map<String, String> request) {
        String orderId = request.get("orderId");
        if (orderId == null || orderId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "INVALID_REQUEST", "orderId is required"));
        }
        inventoryService.releaseReservation(orderId);
        return ResponseEntity.ok().build();
    }
}
