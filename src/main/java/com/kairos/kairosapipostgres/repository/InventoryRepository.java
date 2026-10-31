package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Override
    @EntityGraph(attributePaths = {"sector"})
    List<Inventory> findAll();

    @EntityGraph(attributePaths = {"sector"})
    List<Inventory> findBySectorId(Long sectorId);
}
