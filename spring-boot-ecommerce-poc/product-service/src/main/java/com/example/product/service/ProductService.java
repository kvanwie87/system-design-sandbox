package com.example.product.service;

import com.example.common.dto.ProductDTO;
import com.example.common.util.IdGenerator;
import com.example.product.entity.Product;
import com.example.product.event.ProductEventPublisher;
import com.example.product.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, ProductEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Cacheable(value = "products", key = "#id")
    public Optional<ProductDTO> getProductById(String id) {
        return productRepository.findById(id).map(this::toDTO);
    }

    public Page<ProductDTO> listProducts(String category, Pageable pageable) {
        Page<Product> products;
        if (category != null && !category.isBlank()) {
            products = productRepository.findByCategory(category, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(this::toDTO);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#result.id()")
    public ProductDTO createProduct(ProductDTO dto) {
        Product product = new Product();
        product.setId(IdGenerator.generate("prod"));
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setCategory(dto.category());
        product.setPrice(dto.price());
        product.setImageUrl(dto.imageUrl());
        product.setRating(dto.rating());
        product.setReviewCount(dto.reviewCount());
        product.setSellerId(dto.sellerId());
        product.setAttributes(dto.attributes());

        Product saved = productRepository.save(product);
        ProductDTO result = toDTO(saved);
        eventPublisher.publishProductCreated(result);
        return result;
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public Optional<ProductDTO> updateProduct(String id, ProductDTO dto) {
        return productRepository.findById(id).map(product -> {
            if (dto.name() != null) product.setName(dto.name());
            if (dto.description() != null) product.setDescription(dto.description());
            if (dto.category() != null) product.setCategory(dto.category());
            if (dto.price() != null) product.setPrice(dto.price());
            if (dto.imageUrl() != null) product.setImageUrl(dto.imageUrl());
            if (dto.rating() != null) product.setRating(dto.rating());
            if (dto.reviewCount() != null) product.setReviewCount(dto.reviewCount());
            if (dto.sellerId() != null) product.setSellerId(dto.sellerId());
            if (dto.attributes() != null) product.setAttributes(dto.attributes());

            Product saved = productRepository.save(product);
            ProductDTO result = toDTO(saved);
            eventPublisher.publishProductUpdated(result);
            return result;
        });
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getImageUrl(),
                product.getRating(),
                product.getReviewCount(),
                product.getSellerId(),
                product.getAttributes(),
                product.getCreatedAt() != null ? product.getCreatedAt().toString() : null,
                product.getUpdatedAt() != null ? product.getUpdatedAt().toString() : null
        );
    }
}
