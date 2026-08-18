package com.example.order.saga;

import com.example.common.dto.CartDTO;
import com.example.common.dto.CartItemDTO;
import com.example.common.dto.OrderDTO;
import com.example.common.dto.OrderItemDTO;
import com.example.common.enums.OrderStatus;
import com.example.common.enums.PaymentStatus;
import com.example.common.request.PaymentRequest;
import com.example.common.request.ReserveInventoryRequest;
import com.example.common.response.PaymentResponse;
import com.example.common.response.ReservationResponse;
import com.example.common.util.IdGenerator;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.event.OrderEventPublisher;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Saga orchestrator for the checkout flow.
 * Coordinates: Cart → Inventory Reserve → Payment → Order Create → Inventory Confirm → Cart Clear
 * With compensating actions on each failure point.
 */
@Component
public class CheckoutSaga {

    private static final Logger log = LoggerFactory.getLogger(CheckoutSaga.class);
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("5.99");

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final String cartServiceUrl;
    private final String inventoryServiceUrl;
    private final String paymentServiceUrl;

    public CheckoutSaga(OrderRepository orderRepository,
                        OrderEventPublisher eventPublisher,
                        RestTemplate restTemplate,
                        @Value("${cart-service.url}") String cartServiceUrl,
                        @Value("${inventory-service.url}") String inventoryServiceUrl,
                        @Value("${payment-service.url}") String paymentServiceUrl) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
        this.cartServiceUrl = cartServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Transactional
    public OrderDTO execute(String userId, String shippingAddressId, String idempotencyKey) {
        log.info("Starting checkout saga for user: {}", userId);

        // Step 1: Fetch cart
        CartDTO cart = fetchCart(userId);
        if (cart.items() == null || cart.items().isEmpty()) {
            throw new IllegalStateException("Cart is empty for user: " + userId);
        }

        String orderId = IdGenerator.generate("ord");
        List<String> reservedProducts = new ArrayList<>();
        String paymentId = null;

        try {
            // Step 2: Reserve inventory for each item
            for (CartItemDTO item : cart.items()) {
                reserveInventory(item.productId(), item.quantity(), orderId);
                reservedProducts.add(item.productId());
            }
            log.info("Inventory reserved for {} items", reservedProducts.size());

            // Step 3: Process payment
            BigDecimal subtotal = cart.subtotal();
            BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(tax).add(SHIPPING_FEE);

            PaymentResponse paymentResponse = processPayment(orderId, total, idempotencyKey);
            if (paymentResponse.status() != PaymentStatus.SUCCESS) {
                throw new PaymentFailedException("Payment declined for order: " + orderId);
            }
            paymentId = paymentResponse.paymentId();
            log.info("Payment processed successfully: {}", paymentId);

            // Step 4: Create order record
            Order order = createOrderRecord(orderId, userId, cart, subtotal, tax, total,
                    shippingAddressId, paymentId, idempotencyKey);
            log.info("Order record created: {}", orderId);

            // Step 5: Confirm inventory reservation
            try {
                confirmInventory(orderId);
                log.info("Inventory reservation confirmed for order: {}", orderId);
            } catch (Exception e) {
                // Confirmation failure is non-critical — order is still valid, log for manual retry
                log.error("Failed to confirm inventory for order {}. Manual retry needed.", orderId, e);
            }

            // Step 6: Clear cart
            try {
                clearCart(userId);
                log.info("Cart cleared for user: {}", userId);
            } catch (Exception e) {
                // Cart clear failure is non-critical
                log.warn("Failed to clear cart for user {}. Will expire naturally.", userId, e);
            }

            // Step 7: Publish OrderCreatedEvent to Kafka
            OrderDTO orderDTO = toDTO(order);
            try {
                eventPublisher.publishOrderCreated(orderDTO);
                log.info("Published OrderCreatedEvent for order: {}", orderId);
            } catch (Exception e) {
                // Event publishing failure is non-critical — order is still valid
                log.warn("Failed to publish OrderCreatedEvent for order {}. Will retry later.", orderId, e);
            }

            return orderDTO;

        } catch (PaymentFailedException e) {
            // Compensation: release inventory
            log.warn("Payment failed for order {}. Releasing inventory.", orderId);
            compensateReleaseInventory(orderId);
            throw new IllegalStateException("Payment failed: " + e.getMessage());

        } catch (IllegalStateException e) {
            // Compensation: if payment was made, refund; release inventory
            if (paymentId != null) {
                log.warn("Order creation failed. Refunding payment {} and releasing inventory.", paymentId);
                compensateRefundPayment(paymentId);
            }
            if (!reservedProducts.isEmpty()) {
                compensateReleaseInventory(orderId);
            }
            throw e;
        }
    }

    private CartDTO fetchCart(String userId) {
        String url = cartServiceUrl + "/cart/" + userId;
        CartDTO cart = restTemplate.getForObject(url, CartDTO.class);
        if (cart == null) {
            throw new IllegalStateException("Could not fetch cart for user: " + userId);
        }
        return cart;
    }

    private void reserveInventory(String productId, int quantity, String orderId) {
        String url = inventoryServiceUrl + "/inventory/reserve";
        ReserveInventoryRequest request = new ReserveInventoryRequest(productId, quantity, orderId);
        restTemplate.postForObject(url, request, ReservationResponse.class);
    }

    private PaymentResponse processPayment(String orderId, BigDecimal total, String idempotencyKey) {
        String url = paymentServiceUrl + "/payments/charge";
        PaymentRequest request = new PaymentRequest(orderId, total, "USD", "4242", idempotencyKey);
        PaymentResponse response = restTemplate.postForObject(url, request, PaymentResponse.class);
        if (response == null) {
            throw new IllegalStateException("Null response from payment service");
        }
        return response;
    }

    private Order createOrderRecord(String orderId, String userId, CartDTO cart,
                                    BigDecimal subtotal, BigDecimal tax, BigDecimal total,
                                    String shippingAddressId, String paymentId, String idempotencyKey) {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShippingFee(SHIPPING_FEE);
        order.setTotal(total);
        order.setShippingAddressId(shippingAddressId);
        order.setPaymentId(paymentId);
        order.setIdempotencyKey(idempotencyKey);

        for (CartItemDTO cartItem : cart.items()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(IdGenerator.generate("item"));
            orderItem.setProductId(cartItem.productId());
            orderItem.setProductName(cartItem.productName());
            orderItem.setQuantity(cartItem.quantity());
            orderItem.setUnitPrice(cartItem.unitPrice());
            orderItem.setSubtotal(cartItem.subtotal());
            order.addItem(orderItem);
        }

        return orderRepository.save(order);
    }

    private void confirmInventory(String orderId) {
        String url = inventoryServiceUrl + "/inventory/confirm";
        restTemplate.postForObject(url, Map.of("orderId", orderId), Void.class);
    }

    private void clearCart(String userId) {
        String url = cartServiceUrl + "/cart/" + userId;
        restTemplate.delete(url);
    }

    private void compensateReleaseInventory(String orderId) {
        try {
            String url = inventoryServiceUrl + "/inventory/release";
            restTemplate.postForObject(url, Map.of("orderId", orderId), Void.class);
            log.info("Compensation: inventory released for order {}", orderId);
        } catch (Exception e) {
            log.error("Compensation FAILED: could not release inventory for order {}. Manual intervention required.", orderId, e);
        }
    }

    private void compensateRefundPayment(String paymentId) {
        try {
            String url = paymentServiceUrl + "/payments/refund/" + paymentId;
            restTemplate.postForObject(url, null, PaymentResponse.class);
            log.info("Compensation: payment refunded {}", paymentId);
        } catch (Exception e) {
            log.error("Compensation FAILED: could not refund payment {}. Manual intervention required.", paymentId, e);
        }
    }

    private OrderDTO toDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                )).toList();

        return new OrderDTO(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                itemDTOs,
                order.getSubtotal(),
                order.getTax(),
                order.getShippingFee(),
                order.getTotal(),
                order.getShippingAddressId(),
                order.getPaymentId(),
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null
        );
    }

    private static class PaymentFailedException extends RuntimeException {
        PaymentFailedException(String message) {
            super(message);
        }
    }
}
