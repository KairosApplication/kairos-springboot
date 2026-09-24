package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByShelfId(Long shelfId);
}
