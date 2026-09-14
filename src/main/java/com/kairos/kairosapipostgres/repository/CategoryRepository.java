package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCategory(String category);

    boolean existsByCategory(String category);

}
