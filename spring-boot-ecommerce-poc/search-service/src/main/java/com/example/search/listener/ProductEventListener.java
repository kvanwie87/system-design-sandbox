package com.example.search.listener;

import com.example.common.dto.ProductDTO;
import com.example.common.event.ProductUpdatedEvent;
import com.example.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for product CDC events and keeps the
 * Elasticsearch index in sync with the product catalog.
 */
@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    private final SearchService searchService;

    public ProductEventListener(SearchService searchService) {
        this.searchService = searchService;
    }

    @KafkaListener(topics = "product-events", groupId = "search-service")
    public void handleProductEvent(ProductUpdatedEvent event) {
        log.info("Received product event: action={}, productId={}", event.action(), event.productId());

        switch (event.action()) {
            case "CREATE", "UPDATE" -> {
                if (event.product() != null) {
                    searchService.indexProduct(searchService.fromDTO(event.product()));
                    log.info("Indexed product: {}", event.productId());
                }
            }
            case "DELETE" -> {
                searchService.deleteProduct(event.productId());
                log.info("Removed product from index: {}", event.productId());
            }
            default -> log.warn("Unknown product event action: {}", event.action());
        }
    }
}
