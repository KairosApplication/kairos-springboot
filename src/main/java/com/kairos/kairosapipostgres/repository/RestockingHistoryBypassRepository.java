package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.RestockingHistoryBypass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;

public interface RestockingHistoryBypassRepository extends JpaRepository<RestockingHistoryBypass, Long> {
    boolean existsByCompanyIdAndEmployeeIdAndWeekStart(Long company, Long employee, LocalDate week);
    @Modifying
    @Query("delete from RestockingHistoryBypass b where b.weekStart < :week")
    void deleteByWeekStartBefore(LocalDate week);
}
