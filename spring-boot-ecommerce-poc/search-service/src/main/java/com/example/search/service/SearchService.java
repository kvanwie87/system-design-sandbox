package com.example.search.service;

import com.example.common.dto.ProductDTO;
import com.example.search.document.ProductDocument;
import com.example.search.repository.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.SortOrder;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public SearchService(ProductSearchRepository productSearchRepository,
                         ElasticsearchOperations elasticsearchOperations) {
        this.productSearchRepository = productSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public SearchResult search(String query, String category, BigDecimal minPrice,
                               BigDecimal maxPrice, Double minRating, String sortBy,
                               int page, int limit) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Full-text search on name and description with title boost
        if (query != null && !query.isBlank()) {
            boolBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(query)
                    .fields("name^3", "description")
                    .fuzziness("AUTO")
            ));
        } else {
            boolBuilder.must(m -> m.matchAll(ma -> ma));
        }

        // Category filter
        if (category != null && !category.isBlank()) {
            boolBuilder.filter(f -> f.term(t -> t.field("category").value(category)));
        }

        // Price range filter
        if (minPrice != null || maxPrice != null) {
            boolBuilder.filter(f -> f.range(r -> r.number(n -> {
                var builder = n.field("price");
                if (minPrice != null) builder.gte(minPrice.doubleValue());
                if (maxPrice != null) builder.lte(maxPrice.doubleValue());
                return builder;
            })));
        }

        // Rating filter
        if (minRating != null) {
            boolBuilder.filter(f -> f.range(r -> r.number(n -> n.field("rating").gte(minRating))));
        }

        NativeQueryBuilder queryBuilder = new NativeQueryBuilder()
                .withQuery(q -> q.bool(boolBuilder.build()))
                .withPageable(PageRequest.of(page, limit));

        // Sorting
        if (sortBy != null) {
            switch (sortBy) {
                case "price_asc" -> queryBuilder.withSort(Sort.by(Sort.Direction.ASC, "price"));
                case "price_desc" -> queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "price"));
                case "rating" -> queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "rating"));
                default -> {} // relevance (default ES scoring)
            }
        }

        Query nativeQuery = queryBuilder.build();
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        List<ProductDocument> results = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new SearchResult(results, searchHits.getTotalHits(), page, limit);
    }

    public void indexProduct(ProductDocument document) {
        productSearchRepository.save(document);
        log.debug("Indexed product: {} - {}", document.getId(), document.getName());
    }

    public void deleteProduct(String productId) {
        productSearchRepository.deleteById(productId);
        log.debug("Deleted product from index: {}", productId);
    }

    public long getIndexCount() {
        return productSearchRepository.count();
    }

    public ProductDocument fromDTO(ProductDTO dto) {
        return new ProductDocument(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.category(),
                dto.price(),
                dto.rating(),
                dto.reviewCount(),
                dto.sellerId(),
                dto.imageUrl()
        );
    }

    public record SearchResult(
            List<ProductDocument> products,
            long totalHits,
            int page,
            int limit
    ) {}
}
