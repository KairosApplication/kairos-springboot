package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    Optional<Sector> findByName(String name);

    boolean existsByName (String name);

    List<Sector> findByType(String type);
}
