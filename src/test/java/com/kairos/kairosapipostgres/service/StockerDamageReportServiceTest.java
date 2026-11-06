package com.kairos.kairosapipostgres.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StockerDamageReportServiceTest {
    @Test
    void mapsScopedMetadataAndConfirmsOnlyOpenOrAlreadyResolvedReports() throws Exception {
        JdbcTemplate db = mock(JdbcTemplate.class);
        var service = new StockerDamageReportService(db);
        var auth = new UsernamePasswordAuthenticationToken("stocker@invalid", null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        ResultSet row = mock(ResultSet.class);
        when(row.getLong("id")).thenReturn(91L);
        when(row.getLong("company_id")).thenReturn(2L);
        when(row.getString("report_id")).thenReturn("b8b80b83-b86d-47c8-b8ec-8c0361fe3aa5");
        when(row.getString("problem_type")).thenReturn("DAMAGED");
        when(row.getString("report_description")).thenReturn("Broken package");
        when(row.getString("product_name")).thenReturn("Product");
        when(row.getObject("reported_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.parse("2026-11-05T12:00:00Z"));
        when(db.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<?> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(row, 0));
        });
        assertThat(service.list(auth).getFirst()).containsEntry("reported_at", "2026-11-05T12:00:00Z")
                .containsEntry("problem_type", "DAMAGED");
        when(db.queryForList(contains("FOR UPDATE OF a"), any(Object[].class)))
                .thenReturn(List.of(Map.of("id", 91L, "status", "OPEN")),
                        List.of(Map.of("id", 91L, "status", "RESOLVED")),
                        List.of(Map.of("id", 91L, "status", "CANCELLED")), List.of());
        assertThat(service.resolve(auth, 91L)).containsEntry("status", "RESOLVED");
        assertThat(service.resolve(auth, 91L)).containsEntry("status", "RESOLVED");
        verify(db, times(1)).update(contains("resolution_date=CURRENT_TIMESTAMP"), eq(91L));
        assertThatThrownBy(() -> service.resolve(auth, 91L)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(() -> service.resolve(auth, 91L)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
        when(db.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        assertThatThrownBy(() -> service.list(auth)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.list(null)).isInstanceOf(ResponseStatusException.class);
        var customer = new UsernamePasswordAuthenticationToken("customer@invalid", null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        assertThatThrownBy(() -> service.list(customer)).isInstanceOf(ResponseStatusException.class);
    }
}
