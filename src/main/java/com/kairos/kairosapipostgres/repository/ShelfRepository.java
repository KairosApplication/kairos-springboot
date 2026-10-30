package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

@Repository
public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shelf s where s.id = :id")
    Optional<Shelf> locked(Long id);

    List<Shelf> findByAisleId(Long aisleId);
}
