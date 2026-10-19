package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.RestockingHistoryDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;

public interface RestockingHistoryDeliveryRepository extends JpaRepository<RestockingHistoryDelivery, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from RestockingHistoryDelivery d where d.id = :id")
    Optional<RestockingHistoryDelivery> locked(Long id);
    List<RestockingHistoryDelivery> findByDeliveredFalseAndNextAttemptAtLessThanEqualOrderById(Instant now, Pageable page);
    boolean existsByCompanyIdAndEmployeeIdAndWeekStartAndDeliveredFalse(Long company, Long employee, LocalDate week);
    Optional<RestockingHistoryDelivery> findFirstByRestockingId(Long id);
    void deleteByRestockingId(Long id);
    @Modifying
    @Query("delete from RestockingHistoryDelivery d where d.weekStart < :week")
    void deleteByWeekStartBefore(LocalDate week);
}
