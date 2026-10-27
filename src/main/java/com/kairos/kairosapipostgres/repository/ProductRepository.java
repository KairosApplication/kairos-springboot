package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    interface RecommendedProduct {
        Long getProductId();
        String getProductName();
        Long getCategoryId();
        String getCategoryName();
    }

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT p.id AS "productId", p.name AS "productName",
                   c.id AS "categoryId", c.category AS "categoryName"
            FROM products p
            JOIN categories c ON c.id = p.category_id
            JOIN (
                SELECT p2.category_id, SUM(pp.quantity) AS purchased_quantity
                FROM purchases pu
                JOIN purchase_products pp ON pp.purchase_id = pu.id
                JOIN products p2 ON p2.id = pp.product_id
                WHERE pu.customer_id = :customerId AND pu.status = 'COMPLETED'
                GROUP BY p2.category_id
            ) category_totals ON category_totals.category_id = p.category_id
            WHERE NOT EXISTS (
                SELECT 1 FROM purchases pu2
                JOIN purchase_products pp2 ON pp2.purchase_id = pu2.id
                WHERE pu2.customer_id = :customerId
                  AND pu2.status = 'COMPLETED' AND pp2.product_id = p.id
            )
            ORDER BY category_totals.purchased_quantity DESC, p.name ASC, p.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<RecommendedProduct> recommendForCustomer(
            @org.springframework.data.repository.query.Param("customerId") Long customerId,
            @org.springframework.data.repository.query.Param("limit") int limit
    );

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    List<Product> findByBrand(String brand);

    @Query(value = "SELECT * FROM products p WHERE CAST(p.price AS NUMERIC) <= :price", nativeQuery = true)
    List<Product> findByPriceLessThanEqual(@Param("price") BigDecimal price);

}
