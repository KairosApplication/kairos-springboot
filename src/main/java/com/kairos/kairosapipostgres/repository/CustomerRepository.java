package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.repository.common.UserOwnedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository
        extends UserOwnedRepository<Customer, Long> {
}
