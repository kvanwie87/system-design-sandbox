package com.example.inventory.repository;

import com.example.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    List<Inventory> findByProductId(String productId);

    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.warehouseId = :warehouseId")
    Optional<Inventory> findByProductIdAndWarehouseId(@Param("productId") String productId,
                                                      @Param("warehouseId") String warehouseId);

    @Query("SELECT COALESCE(SUM(i.availableQty), 0) FROM Inventory i WHERE i.productId = :productId")
    int getTotalAvailableQty(@Param("productId") String productId);
}
