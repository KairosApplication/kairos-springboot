package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByUserId(Long userId);
}
