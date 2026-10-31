package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.repository.common.UserOwnedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

import java.util.Optional;

@Repository
public interface CustomerRepository
        extends UserOwnedRepository<Customer, Long> {
    @Override
    @EntityGraph(attributePaths = {"user"})
    List<Customer> findAll();
}
