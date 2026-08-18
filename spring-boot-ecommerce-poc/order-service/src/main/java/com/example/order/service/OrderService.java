package com.example.order.service;

import com.example.common.dto.OrderDTO;
import com.example.common.dto.OrderItemDTO;
import com.example.common.request.CreateOrderRequest;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.saga.CheckoutSaga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CheckoutSaga checkoutSaga;

    public OrderService(OrderRepository orderRepository, CheckoutSaga checkoutSaga) {
        this.orderRepository = orderRepository;
        this.checkoutSaga = checkoutSaga;
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        // Idempotency check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent order request detected for key: {}. Returning existing order.",
                        request.idempotencyKey());
                return toDTO(existing.get());
            }
        }

        // Execute the checkout saga
        return checkoutSaga.execute(request.userId(), request.shippingAddressId(), request.idempotencyKey());
    }

    public Optional<OrderDTO> getOrder(String orderId) {
        return orderRepository.findById(orderId).map(this::toDTO);
    }

    public List<OrderDTO> getOrdersByUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .toList();
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
}
