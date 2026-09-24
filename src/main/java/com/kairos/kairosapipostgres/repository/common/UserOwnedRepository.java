package com.kairos.kairosapipostgres.repository.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface UserOwnedRepository<T, ID> extends JpaRepository<T,ID> {

    boolean existsByUserId (Long userId);

    Optional<T> findByUserId(Long userId);
}
