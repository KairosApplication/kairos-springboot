package com.kairos.kairosapipostgres.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@Table(name = "restocking_history_delivery", indexes = @Index(name = "idx_history_delivery_pending", columnList = "delivered,next_attempt_at"))
public class RestockingHistoryDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long restockingId;
    private Long companyId;
    private Long employeeId;
    private LocalDate weekStart;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    private boolean delivered;
    private Instant nextAttemptAt;
}
