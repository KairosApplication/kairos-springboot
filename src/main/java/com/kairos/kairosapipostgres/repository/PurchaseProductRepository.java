package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.PurchaseProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PurchaseProductRepository extends JpaRepository<PurchaseProduct, Long> {
    @Query(value = """
            SELECT COALESCE(SUM(CAST(pp.amount AS DECIMAL(18, 2)) * pp.quantity), 0)
            FROM purchase_products pp
            WHERE pp.purchase_id = :purchaseId
            """, nativeQuery = true)
    BigDecimal calculateTotal(@Param("purchaseId") Long purchaseId);
}
