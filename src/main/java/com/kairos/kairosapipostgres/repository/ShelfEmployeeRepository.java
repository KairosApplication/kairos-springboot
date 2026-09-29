package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.ShelfEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShelfEmployeeRepository extends JpaRepository<ShelfEmployee, Long> {
    Optional<ShelfEmployee> findByEmployeeIdAndShelfId(Long employeeId, Long shelfId);
    List<ShelfEmployee> findByEmployeeId(Long employeeId);
    List<ShelfEmployee> findByShelfId(Long shelfId);
}
