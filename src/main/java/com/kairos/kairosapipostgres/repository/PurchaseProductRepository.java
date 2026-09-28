package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.PurchaseProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PurchaseProductRepository extends JpaRepository<PurchaseProduct, Long> {
    @Query("select sum(pp.amount * pp.quantity) from PurchaseProduct pp where pp.purchase.id = :purchaseId")
    BigDecimal calculateTotal(@Param("purchaseId") Long purchaseId);
}
