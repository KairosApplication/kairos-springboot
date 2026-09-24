package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByShelfId(Long shelfId);
}
