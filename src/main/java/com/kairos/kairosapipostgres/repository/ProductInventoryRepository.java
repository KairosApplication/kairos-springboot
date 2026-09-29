package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
    boolean existsByInventoryIdAndProductIdAndProductQuantityGreaterThanEqual(
            Long inventoryId, Long productId, Integer quantity
    );
}
