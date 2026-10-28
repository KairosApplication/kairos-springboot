package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    @Override
    @EntityGraph(attributePaths = {"aisle"})
    List<Shelf> findAll();

    List<Shelf> findByAisleId(Long aisleId);

    @EntityGraph(attributePaths = {"aisle"})
    List<Shelf> findByAisleSectorCompanyId(Long companyId);
}
