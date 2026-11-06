package com.kairos.kairosapipostgres.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class StockerDamageReportService {
    private final JdbcTemplate db;

    public StockerDamageReportService(JdbcTemplate db) {
        this.db = db;
    }

    private record Actor(long employeeId, long companyId) { }

    private Actor actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(role -> "ROLE_EMPLOYEE".equals(role.getAuthority())
                        || "ROLE_MANAGER".equals(role.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var rows = db.query("""
                SELECT e.id,e.company_id FROM employees e JOIN users u ON u.id=e.users_id
                WHERE u.email=? AND e.position IN ('STOCKER','MANAGER')
                """, (rs, index) -> new Actor(rs.getLong("id"), rs.getLong("company_id")), auth.getName());
        if (rows.size() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return rows.getFirst();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Authentication auth) {
        var owner = actor(auth);
        return db.query("""
                SELECT a.id,r.id report_id,r.problem_type,r.description report_description,
                       p.name product_name,r.created_at reported_at
                FROM customer_damage_reports r JOIN alerts a ON a.id=r.alert_id
                JOIN products p ON p.id=r.product_id
                JOIN shelves sh ON sh.id=a.shelf_id JOIN aisles ai ON ai.id=sh.aisle_id
                JOIN sectors s ON s.id=ai.sector_id
                WHERE a.employee_id=? AND r.company_id=? AND s.company_id=?
                  AND p.company_id=? AND a.status='OPEN'
                ORDER BY r.created_at DESC,a.id DESC
                """, (rs, index) -> Map.<String, Object>of(
                "id", rs.getLong("id"), "report_id", rs.getString("report_id"),
                "problem_type", rs.getString("problem_type"),
                "report_description", rs.getString("report_description"),
                "product_name", rs.getString("product_name"),
                "reported_at", rs.getObject("reported_at", OffsetDateTime.class).toInstant().toString()),
                owner.employeeId(), owner.companyId(), owner.companyId(), owner.companyId());
    }

    @Transactional
    public Map<String, Object> resolve(Authentication auth, long alertId) {
        var owner = actor(auth);
        var rows = db.queryForList("""
                SELECT a.id,a.status FROM customer_damage_reports r JOIN alerts a ON a.id=r.alert_id
                JOIN products p ON p.id=r.product_id
                JOIN shelves sh ON sh.id=a.shelf_id JOIN aisles ai ON ai.id=sh.aisle_id
                JOIN sectors s ON s.id=ai.sector_id
                WHERE a.id=? AND a.employee_id=? AND r.company_id=? AND s.company_id=? AND p.company_id=?
                FOR UPDATE OF a
                """, alertId, owner.employeeId(), owner.companyId(), owner.companyId(), owner.companyId());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String status = rows.getFirst().get("status").toString();
        if ("OPEN".equals(status)) {
            db.update("UPDATE alerts SET status='RESOLVED',resolution_date=CURRENT_TIMESTAMP WHERE id=?", alertId);
        } else if (!"RESOLVED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O relato foi cancelado");
        }
        return Map.of("alertId", alertId, "status", "RESOLVED");
    }
}
