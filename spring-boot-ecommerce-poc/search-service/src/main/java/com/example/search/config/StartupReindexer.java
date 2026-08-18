package com.example.search.config;

import com.example.common.dto.ProductDTO;
import com.example.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * On startup, checks if the Elasticsearch index is empty.
 * If so, fetches all products from the Product Service and indexes them.
 */
@Configuration
public class StartupReindexer {

    private static final Logger log = LoggerFactory.getLogger(StartupReindexer.class);

    @Value("${product-service.url:http://localhost:8081}")
    private String productServiceUrl;

    @Bean
    CommandLineRunner reindexOnStartup(SearchService searchService) {
        return args -> {
            try {
                long count = searchService.getIndexCount();
                if (count > 0) {
                    log.info("Elasticsearch index already has {} products. Skipping reindex.", count);
                    return;
                }

                log.info("Elasticsearch index is empty. Triggering full reindex from Product Service...");
                RestTemplate restTemplate = new RestTemplate();

                // Fetch products from product service (paginated, get first page of 100)
                String url = productServiceUrl + "/products?page=0&size=100";
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

                if (response.getBody() != null && response.getBody().containsKey("content")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> products = (List<Map<String, Object>>) response.getBody().get("content");

                    for (Map<String, Object> product : products) {
                        ProductDTO dto = mapToProductDTO(product);
                        searchService.indexProduct(searchService.fromDTO(dto));
                    }
                    log.info("Reindexed {} products from Product Service.", products.size());
                } else {
                    log.warn("No products returned from Product Service for reindex.");
                }
            } catch (Exception e) {
                log.warn("Could not perform startup reindex (Product Service may not be available): {}",
                        e.getMessage());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private ProductDTO mapToProductDTO(Map<String, Object> map) {
        return new ProductDTO(
                (String) map.get("id"),
                (String) map.get("name"),
                (String) map.get("description"),
                (String) map.get("category"),
                map.get("price") != null ? new java.math.BigDecimal(map.get("price").toString()) : null,
                (String) map.get("imageUrl"),
                map.get("rating") != null ? ((Number) map.get("rating")).doubleValue() : null,
                map.get("reviewCount") != null ? ((Number) map.get("reviewCount")).intValue() : null,
                (String) map.get("sellerId"),
                (Map<String, Object>) map.get("attributes"),
                null, null
        );
    }
}
