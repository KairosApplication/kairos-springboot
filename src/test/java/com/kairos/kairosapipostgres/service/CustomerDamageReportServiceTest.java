package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.CustomerDamageReportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerDamageReportServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final CustomerDamageReportService service = new CustomerDamageReportService(db);
    private final Authentication customer = new UsernamePasswordAuthenticationToken("customer@invalid", null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    private final UUID protocol = UUID.randomUUID();

    @BeforeEach
    void ownerAndAvailableProduct() {
        when(db.queryForList(contains("SELECT c.id,c.company_id"), eq("customer@invalid")))
                .thenReturn(List.of(Map.of("id", 1L, "company_id", 2L, "name", "Test market")));
        when(db.queryForList(contains("SELECT id FROM products"), eq(17L), eq(2L)))
                .thenReturn(List.of(Map.of("id", 17)));
        when(db.queryForMap(contains("SELECT r.id,r.alert_id"), any(UUID.class)))
                .thenReturn(Map.of("id", protocol, "alert_id", 91, "status", "OPEN"));
    }

    private CustomerDamageReportRequest request(UUID command, String photo) {
        return new CustomerDamageReportRequest(command, 17L, 3L, "DAMAGED", " Broken package ", photo);
    }

    @Test
    void confirmsOnlyAfterWriteUsingAuthenticatedCustomerAndCompany() {
        UUID command = UUID.randomUUID();
        assertThat(service.report(customer, request(command, null)))
                .isEqualTo(Map.of("protocol", protocol.toString(), "alertId", 91, "status", "OPEN"));
        verify(db).update(contains("INSERT INTO customer_damage_reports"), any(UUID.class), eq(command),
                eq(1L), eq(2L), eq(17L), eq(3L), eq("DAMAGED"), eq("Broken package"), isNull(), anyString());
    }

    @Test
    void rejectsWrongRoleUnknownOwnerForeignProductAndFailedRouting() {
        assertThatThrownBy(() -> service.report(null, request(UUID.randomUUID(), null)))
                .isInstanceOf(ResponseStatusException.class);
        var employee = new UsernamePasswordAuthenticationToken("employee@invalid", null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        assertThatThrownBy(() -> service.options(employee)).isInstanceOf(ResponseStatusException.class);
        when(db.queryForList(contains("SELECT id FROM products"), eq(17L), eq(2L))).thenReturn(List.of());
        assertThatThrownBy(() -> service.report(customer, request(UUID.randomUUID(), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode().value()).isEqualTo(403));
        when(db.queryForList(contains("SELECT id FROM products"), eq(17L), eq(2L)))
                .thenReturn(List.of(Map.of("id", 17)));
        when(db.update(contains("INSERT INTO customer_damage_reports"), any(Object[].class)))
                .thenThrow(new DataIntegrityViolationException("No assigned stocker"));
        assertThatThrownBy(() -> service.report(customer, request(UUID.randomUUID(), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode().value()).isEqualTo(409));
        when(db.queryForList(contains("SELECT c.id,c.company_id"), eq("customer@invalid"))).thenReturn(List.of());
        assertThatThrownBy(() -> service.capabilities(customer)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void exposesCapabilitiesScopedListsAndExplicitCatalogTruncation() {
        assertThat(service.capabilities(customer)).containsEntry("companyId", 2L).containsEntry("photoAvailable", true);
        when(db.queryForList(contains("SELECT id,name"), eq(2L)))
                .thenReturn(Collections.nCopies(1001, Map.of("id", 17)));
        when(db.queryForList(contains("FROM product_shelves ps"), eq(2L), eq(2L)))
                .thenReturn(Collections.nCopies(10001, Map.of("shelf_id", 3)));
        var options = service.options(customer);
        assertThat(options).containsEntry("catalogTruncated", true).containsEntry("locationsTruncated", true);
        assertThat((List<?>) options.get("products")).hasSize(1000);
        assertThat((List<?>) options.get("locations")).hasSize(10000);
        assertThat(service.list(customer)).isEmpty();
        verify(db).queryForList(contains("WHERE r.customer_id=? AND r.company_id=?"), eq(1L), eq(2L));
    }

    @Test
    void validatesAndReencodesOptionalJpegBeforeAnyInsert() throws Exception {
        for (String photo : List.of("", "bm90LWEtcGhvdG8=", "invalid-base64!", "A".repeat(4194305))) {
            assertThatThrownBy(() -> service.report(customer, request(UUID.randomUUID(), photo)))
                    .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                            assertThat(error.getStatusCode().value()).isEqualTo(400));
        }
        verify(db, never()).update(contains("INSERT INTO customer_damage_reports"), any(Object[].class));
        var image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", bytes);
        assertThat(service.report(customer, request(UUID.randomUUID(), Base64.getEncoder().encodeToString(bytes.toByteArray()))))
                .containsEntry("status", "OPEN");
    }

    @Test
    void reusedCommandWithDifferentContentsIsRejectedBeforeAnotherInsert() {
        when(db.queryForList(contains("SELECT r.id,r.fingerprint"), eq(1L), any(UUID.class)))
                .thenReturn(List.of(Map.of("fingerprint", "different-content")));
        assertThatThrownBy(() -> service.report(customer, request(UUID.randomUUID(), null)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode().value()).isEqualTo(409));
        verify(db, never()).update(contains("INSERT INTO customer_damage_reports"), any(Object[].class));
    }
}
