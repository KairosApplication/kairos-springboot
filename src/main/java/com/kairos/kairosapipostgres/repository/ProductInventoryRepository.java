package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductInventory p where p.inventory.id = :inventoryId and p.product.id = :productId")
    Optional<ProductInventory> locked(Long inventoryId, Long productId);

    boolean existsByInventoryIdAndProductIdAndProductQuantityGreaterThanEqual(
            Long inventoryId, Long productId, Integer quantity
    );
}
