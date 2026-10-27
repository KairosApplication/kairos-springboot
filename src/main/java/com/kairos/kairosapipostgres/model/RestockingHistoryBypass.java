package com.kairos.kairosapipostgres.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@Table(name = "restocking_history_bypass", indexes = @Index(name = "idx_history_bypass_scope", columnList = "company_id,employee_id,week_start"))
public class RestockingHistoryBypass {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long companyId;
    private Long employeeId;
    private LocalDate weekStart;
}
