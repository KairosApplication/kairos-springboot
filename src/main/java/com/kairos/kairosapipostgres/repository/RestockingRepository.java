package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Restocking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestockingRepository extends JpaRepository<Restocking, Long> {
    List<Restocking> findByShelfId(Long shelfId);
    List<Restocking> findByInventoryId(Long inventoryId);
}
