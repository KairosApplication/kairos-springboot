package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.repository.common.UserOwnedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends
        UserOwnedRepository<Employee, Long> {
}
