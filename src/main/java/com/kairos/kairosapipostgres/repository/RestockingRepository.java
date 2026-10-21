package com.kairos.kairosapipostgres.repository;

import com.kairos.kairosapipostgres.model.Restocking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

@Repository
public interface RestockingRepository extends JpaRepository<Restocking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restocking r where r.id = :id")
    Optional<Restocking> locked(Long id);
    List<Restocking> findByEmployeeCompanyId(Long companyId);

    String WEEK_SCOPE = " from Restocking r where r.employee.company.id = :company and r.employee.id = :employee"
            + " and r.inventory.company.id = :company and r.product.company.id = :company"
            + " and r.shelf.aisle.sector.company.id = :company"
            + " and r.restockingDate >= :start and r.restockingDate < :end";

    @Query("select count(r)" + WEEK_SCOPE)
    long countWeek(Long company, Long employee, LocalDateTime start, LocalDateTime end);

    @Query("select r" + WEEK_SCOPE + " order by r.restockingDate desc, r.id desc")
    @EntityGraph(attributePaths = {"employee.company", "product"})
    List<Restocking> week(Long company, Long employee, LocalDateTime start, LocalDateTime end, Pageable pageable);
    List<Restocking> findByShelfId(Long shelfId);
    List<Restocking> findByInventoryId(Long inventoryId);
}
