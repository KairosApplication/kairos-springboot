package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.common.UserOwnedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends
        UserOwnedRepository<Employee, Long> {
    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Employee> findAll();
    @EntityGraph(attributePaths = {"user"})
    List<Employee> findByCompanyIdAndPositionIn(Long companyId, List<Position> positions);
    boolean existsByPosition(Position position);
    @EntityGraph(attributePaths = {"user", "company"})
    Optional<Employee> findByUserEmail(String email);
}
