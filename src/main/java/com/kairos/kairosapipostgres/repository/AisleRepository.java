package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Aisle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AisleRepository extends JpaRepository<Aisle, Long> {
    @Override
    @EntityGraph(attributePaths = {"sector"})
    List<Aisle> findAll();


    boolean existsBySectorIdAndPosition(Long sectorId, Integer position);
    Optional<Aisle> findBySectorIdAndPosition(Long sectorId, Integer position);
    List<Aisle> findBySectorIdOrderByPosition(Long sectorId);

    @EntityGraph(attributePaths = {"sector"})
    List<Aisle> findBySectorCompanyId(Long companyId);
}
