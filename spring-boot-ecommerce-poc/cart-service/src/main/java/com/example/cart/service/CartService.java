package com.example.cart.service;

import com.example.cart.model.CartItem;
import com.example.common.dto.CartDTO;
import com.example.common.dto.CartItemDTO;
import com.example.common.dto.InventoryDTO;
import com.example.common.request.AddToCartRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String CART_KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final Duration cartTtl;
    private final String inventoryServiceUrl;

    public CartService(RedisTemplate<String, Object> redisTemplate,
                       RestTemplate restTemplate,
                       @Value("${cart.ttl-hours:24}") int ttlHours,
                       @Value("${inventory-service.url}") String inventoryServiceUrl) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.cartTtl = Duration.ofHours(ttlHours);
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    public CartDTO addItem(String userId, AddToCartRequest request) {
        // Validate inventory availability
        validateInventory(request.productId(), request.quantity());

        String cartKey = getCartKey(userId);
        CartItem item = new CartItem(
                request.productId(),
                request.productName(),
                request.unitPrice(),
                request.quantity()
        );

        // Check if item already exists in cart
        Object existing = redisTemplate.opsForHash().get(cartKey, request.productId());
        if (existing != null) {
            CartItem existingItem = convertToCartItem(existing);
            item.setQuantity(existingItem.getQuantity() + request.quantity());
        }

        redisTemplate.opsForHash().put(cartKey, request.productId(), item);
        refreshTtl(cartKey);

        return getCart(userId);
    }

    public CartDTO updateItem(String userId, String productId, int quantity) {
        String cartKey = getCartKey(userId);

        Object existing = redisTemplate.opsForHash().get(cartKey, productId);
        if (existing == null) {
            throw new IllegalArgumentException("Product " + productId + " not found in cart");
        }

        if (quantity <= 0) {
            redisTemplate.opsForHash().delete(cartKey, productId);
        } else {
            CartItem item = convertToCartItem(existing);
            item.setQuantity(quantity);
            redisTemplate.opsForHash().put(cartKey, productId, item);
        }

        refreshTtl(cartKey);
        return getCart(userId);
    }

    public CartDTO removeItem(String userId, String productId) {
        String cartKey = getCartKey(userId);
        redisTemplate.opsForHash().delete(cartKey, productId);
        refreshTtl(cartKey);
        return getCart(userId);
    }

    public CartDTO getCart(String userId) {
        String cartKey = getCartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);

        List<CartItemDTO> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Object value : entries.values()) {
            CartItem item = convertToCartItem(value);
            BigDecimal itemSubtotal = item.getSubtotal();
            items.add(new CartItemDTO(
                    item.getProductId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    itemSubtotal
            ));
            subtotal = subtotal.add(itemSubtotal);
        }

        return new CartDTO(userId, items, subtotal, items.size());
    }

    public void clearCart(String userId) {
        String cartKey = getCartKey(userId);
        redisTemplate.delete(cartKey);
        log.info("Cleared cart for user: {}", userId);
    }

    private void validateInventory(String productId, int quantity) {
        try {
            String url = inventoryServiceUrl + "/inventory/" + productId + "/availability";
            InventoryDTO inventory = restTemplate.getForObject(url, InventoryDTO.class);

            if (inventory == null || inventory.availableQty() < quantity) {
                int available = inventory != null ? inventory.availableQty() : 0;
                throw new IllegalStateException(
                        "Insufficient stock for product " + productId +
                                ". Requested: " + quantity + ", available: " + available);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not validate inventory for product {}: {}", productId, e.getMessage());
            // Allow adding to cart if inventory service is unavailable (graceful degradation)
        }
    }

    private String getCartKey(String userId) {
        return CART_KEY_PREFIX + userId;
    }

    private void refreshTtl(String cartKey) {
        redisTemplate.expire(cartKey, cartTtl);
    }

    @SuppressWarnings("unchecked")
    private CartItem convertToCartItem(Object obj) {
        if (obj instanceof CartItem cartItem) {
            return cartItem;
        }
        // When deserialized from Redis with GenericJackson2JsonRedisSerializer,
        // it may come back as a LinkedHashMap
        if (obj instanceof Map<?, ?> map) {
            CartItem item = new CartItem();
            item.setProductId((String) map.get("productId"));
            item.setProductName((String) map.get("productName"));
            item.setQuantity(((Number) map.get("quantity")).intValue());
            Object price = map.get("unitPrice");
            if (price instanceof Number num) {
                item.setUnitPrice(BigDecimal.valueOf(num.doubleValue()));
            } else {
                item.setUnitPrice(new BigDecimal(price.toString()));
            }
            return item;
        }
        throw new IllegalStateException("Cannot convert " + obj.getClass() + " to CartItem");
    }
}
