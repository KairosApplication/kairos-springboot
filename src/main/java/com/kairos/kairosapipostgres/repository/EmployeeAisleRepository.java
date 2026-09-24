package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.EmployeeAisle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAisleRepository extends JpaRepository<EmployeeAisle, Long> {
    Optional<EmployeeAisle> findByEmployeeIdAndAisleId(Long employeeId, Long aisleId);
    List<EmployeeAisle> findByEmployeeId(Long employeeId);
    List<EmployeeAisle> findByAisleId(Long aisleId);
}
