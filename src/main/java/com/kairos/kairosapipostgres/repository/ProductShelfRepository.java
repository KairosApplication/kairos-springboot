package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.ProductShelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

@Repository
public interface ProductShelfRepository extends JpaRepository<ProductShelf, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductShelf p where p.product.id = :productId and p.shelf.id = :shelfId")
    Optional<ProductShelf> locked(Long productId, Long shelfId);

    Optional<ProductShelf> findByProductIdAndShelfId(Long productId, Long shelfId);
    List<ProductShelf> findByProductId(Long productId);
    List<ProductShelf> findByShelfId(Long shelfId);

    List<ProductShelf> findByShelfAisleSectorCompanyIdAndProductCompanyId(Long shelfCompanyId, Long productCompanyId);
}
