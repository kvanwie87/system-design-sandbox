package com.example.inventory.config;

import com.example.common.util.IdGenerator;
import com.example.inventory.entity.Inventory;
import com.example.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // These product IDs won't match the generated UUIDs from product-service seeder,
    // but for the PoC we'll use well-known IDs that both services can reference.
    // In production, inventory would be populated via events from the product service.
    private static final String[] SAMPLE_PRODUCT_IDS = {
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
    CommandLineRunner seedInventory(InventoryRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                log.info("Inventory already seeded, skipping.");
                return;
            }

            List<Inventory> inventoryList = new ArrayList<>();
            for (String productId : SAMPLE_PRODUCT_IDS) {
                Inventory inv = new Inventory();
                inv.setId(IdGenerator.generate("inv"));
                inv.setProductId(productId);
                inv.setWarehouseId("warehouse-east");
                inv.setAvailableQty(100);
                inv.setReservedQty(0);
                inventoryList.add(inv);
            }

            repository.saveAll(inventoryList);
            log.info("Seeded inventory for {} products.", inventoryList.size());
        };
    }
}
