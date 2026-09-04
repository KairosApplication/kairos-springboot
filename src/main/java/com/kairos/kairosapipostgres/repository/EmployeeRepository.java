package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByUserId(Long userId);
}
