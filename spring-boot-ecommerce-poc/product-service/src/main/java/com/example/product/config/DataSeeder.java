package com.example.product.config;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] PRODUCT_IDS = {
            "prod-wireless-headphones",
            "prod-smart-watch",
            "prod-running-shoes",
            "prod-coffee-maker",
            "prod-backpack",
            "prod-bluetooth-speaker",
            "prod-yoga-mat",
            "prod-desk-lamp",
            "prod-wireless-earbuds",
            "prod-water-bottle",
            "prod-mechanical-keyboard",
            "prod-resistance-bands",
            "prod-ceramic-plant-pot",
            "prod-sunglasses",
            "prod-portable-charger"
    };

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                log.info("Products already seeded, skipping.");
                return;
            }

            List<Product> products = List.of(
                    createProduct(PRODUCT_IDS[0], "Wireless Headphones", "Premium noise-cancelling wireless headphones with 30hr battery",
                            "Electronics", new BigDecimal("149.99"), 4.5, 2341, "seller_001",
                            Map.of("brand", "SoundMax", "connectivity", "Bluetooth 5.3", "battery", "30 hours")),
                    createProduct(PRODUCT_IDS[1], "Smart Watch", "Fitness tracking smartwatch with heart rate monitor and GPS",
                            "Electronics", new BigDecimal("299.99"), 4.3, 1823, "seller_002",
                            Map.of("brand", "TechFit", "display", "AMOLED", "waterResistance", "5ATM")),
                    createProduct(PRODUCT_IDS[2], "Running Shoes", "Lightweight performance running shoes with responsive cushioning",
                            "Fashion", new BigDecimal("89.99"), 4.7, 3421, "seller_003",
                            Map.of("brand", "SprintPro", "material", "Mesh", "weight", "250g")),
                    createProduct(PRODUCT_IDS[3], "Coffee Maker", "Programmable drip coffee maker with thermal carafe",
                            "Home", new BigDecimal("79.99"), 4.2, 892, "seller_004",
                            Map.of("brand", "BrewMaster", "capacity", "12 cups", "type", "Drip")),
                    createProduct(PRODUCT_IDS[4], "Backpack", "Durable travel backpack with laptop compartment and USB charging port",
                            "Fashion", new BigDecimal("59.99"), 4.4, 1567, "seller_005",
                            Map.of("brand", "TrekGear", "volume", "35L", "material", "Nylon")),
                    createProduct(PRODUCT_IDS[5], "Bluetooth Speaker", "Portable waterproof bluetooth speaker with 360-degree sound",
                            "Electronics", new BigDecimal("49.99"), 4.6, 2156, "seller_001",
                            Map.of("brand", "SoundMax", "waterproof", "IPX7", "battery", "12 hours")),
                    createProduct(PRODUCT_IDS[6], "Yoga Mat", "Non-slip eco-friendly yoga mat with alignment markers",
                            "Sports", new BigDecimal("29.99"), 4.5, 743, "seller_006",
                            Map.of("brand", "ZenFlex", "thickness", "6mm", "material", "Natural rubber")),
                    createProduct(PRODUCT_IDS[7], "Desk Lamp", "LED desk lamp with adjustable brightness and color temperature",
                            "Home", new BigDecimal("34.99"), 4.1, 421, "seller_004",
                            Map.of("brand", "LightWell", "lumens", "800", "colorTemps", "2700K-6500K")),
                    createProduct(PRODUCT_IDS[8], "Wireless Earbuds", "True wireless earbuds with active noise cancellation",
                            "Electronics", new BigDecimal("79.99"), 4.4, 1892, "seller_002",
                            Map.of("brand", "TechFit", "driver", "11mm", "anc", "Hybrid ANC")),
                    createProduct(PRODUCT_IDS[9], "Stainless Steel Water Bottle", "Insulated water bottle that keeps drinks cold 24hrs",
                            "Sports", new BigDecimal("24.99"), 4.8, 3201, "seller_006",
                            Map.of("brand", "HydroKeep", "capacity", "750ml", "insulation", "Double-wall vacuum")),
                    createProduct(PRODUCT_IDS[10], "Mechanical Keyboard", "RGB mechanical keyboard with hot-swappable switches",
                            "Electronics", new BigDecimal("129.99"), 4.6, 1456, "seller_001",
                            Map.of("brand", "KeyCraft", "switches", "Cherry MX Brown", "layout", "TKL")),
                    createProduct(PRODUCT_IDS[11], "Resistance Bands Set", "Set of 5 resistance bands for home workouts",
                            "Sports", new BigDecimal("19.99"), 4.3, 2876, "seller_006",
                            Map.of("brand", "FlexFit", "levels", "5", "material", "Natural latex")),
                    createProduct(PRODUCT_IDS[12], "Ceramic Plant Pot", "Modern minimalist ceramic pot for indoor plants",
                            "Home", new BigDecimal("22.99"), 4.0, 654, "seller_007",
                            Map.of("brand", "GreenSpace", "size", "8 inch", "drainage", "Yes")),
                    createProduct(PRODUCT_IDS[13], "Sunglasses", "Polarized UV400 sunglasses with titanium frame",
                            "Fashion", new BigDecimal("69.99"), 4.2, 1123, "seller_005",
                            Map.of("brand", "ClearView", "lens", "Polarized", "frame", "Titanium")),
                    createProduct(PRODUCT_IDS[14], "Portable Charger", "20000mAh power bank with fast charging and USB-C",
                            "Electronics", new BigDecimal("39.99"), 4.5, 2987, "seller_002",
                            Map.of("brand", "PowerMax", "capacity", "20000mAh", "ports", "USB-C + 2x USB-A"))
            );

            repository.saveAll(products);
            log.info("Seeded {} products into the database.", products.size());
        };
    }

    private Product createProduct(String id, String name, String description, String category,
                                  BigDecimal price, double rating, int reviewCount,
                                  String sellerId, Map<String, Object> attributes) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setPrice(price);
        product.setRating(rating);
        product.setReviewCount(reviewCount);
        product.setSellerId(sellerId);
        product.setAttributes(attributes);
        product.setImageUrl("https://placeholder.example.com/products/" + name.toLowerCase().replace(" ", "-") + ".jpg");
        return product;
    }
}
