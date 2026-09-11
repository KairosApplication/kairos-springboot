package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByUserId(Long userId);
}
