package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    List<Product> findByBrand(String brand);

    List<Product> findByPriceLessThanEqual(BigDecimal price);

}
