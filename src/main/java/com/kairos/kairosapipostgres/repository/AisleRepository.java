package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Aisle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AisleRepository extends JpaRepository<Aisle, Long> {

    boolean existsBySectorIdAndPosition(Long sectorId, Integer position);
    Optional<Aisle> findBySectorIdAndPosition(Long sectorId, Integer position);
    List<Aisle> findBySectorIdOrderByPosition(Long sectorId);
}
