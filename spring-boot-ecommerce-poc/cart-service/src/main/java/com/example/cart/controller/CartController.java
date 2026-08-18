package com.example.cart.controller;

import com.example.cart.service.CartService;
import com.example.common.dto.CartDTO;
import com.example.common.exception.ApiError;
import com.example.common.request.AddToCartRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartDTO> getCart(@PathVariable String userId) {
        CartDTO cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addItem(@PathVariable String userId,
                                     @Valid @RequestBody AddToCartRequest request) {
        try {
            CartDTO cart = cartService.addItem(userId, request);
            return ResponseEntity.ok(cart);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(409, "INSUFFICIENT_STOCK", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> updateItem(@PathVariable String userId,
                                        @PathVariable String productId,
                                        @RequestBody Map<String, Integer> body) {
        int quantity = body.getOrDefault("quantity", 0);
        try {
            CartDTO cart = cartService.updateItem(userId, productId, quantity);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(404, "ITEM_NOT_FOUND", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartDTO> removeItem(@PathVariable String userId,
                                              @PathVariable String productId) {
        CartDTO cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
