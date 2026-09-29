package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.ProductShelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductShelfRepository extends JpaRepository<ProductShelf, Long> {
    Optional<ProductShelf> findByProductIdAndShelfId(Long productId, Long shelfId);
    List<ProductShelf> findByProductId(Long productId);
    List<ProductShelf> findByShelfId(Long shelfId);
}
