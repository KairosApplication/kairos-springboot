package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.AlertRequest;
import com.kairos.kairosapipostgres.dto.request.AlertUpdateRequest;
import com.kairos.kairosapipostgres.exception.AlertNotFoundException;
import com.kairos.kairosapipostgres.exception.EmployeeNotFoundException;
import com.kairos.kairosapipostgres.exception.ProductNotFoundException;
import com.kairos.kairosapipostgres.exception.ShelfNotFoundException;
import com.kairos.kairosapipostgres.model.Alert;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.model.enums.AlertStatus;
import com.kairos.kairosapipostgres.repository.AlertRepository;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.ProductRepository;
import com.kairos.kairosapipostgres.repository.ShelfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    @Mock private AlertRepository alerts;
    @Mock private EmployeeRepository employees;
    @Mock private ShelfRepository shelves;
    @Mock private ProductRepository products;

    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(alerts, employees, shelves, products);
    }

    @Test
    void saveResolvesOptionalReferencesAndTrimsDescription() {
        Shelf shelf = shelf(1L);
        Employee employee = employee(2L);
        Product product = product(3L);
        when(shelves.findById(1L)).thenReturn(Optional.of(shelf));
        when(employees.findById(2L)).thenReturn(Optional.of(employee));
        when(products.findById(3L)).thenReturn(Optional.of(product));
        when(alerts.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.save(new AlertRequest(LocalDate.of(2026, 9, 24), 2L, 1L, 3L,
                "  Low stock  ", AlertStatus.OPEN, null));

        assertThat(response.employeeId()).isEqualTo(2L);
        assertThat(response.shelfId()).isEqualTo(1L);
        assertThat(response.productId()).isEqualTo(3L);
        assertThat(response.description()).isEqualTo("Low stock");
    }

    @Test
    void updateAppliesProvidedFieldsAndKeepsOthers() {
        Alert alert = new Alert(4L, null, null, shelf(1L), null, "Old", AlertStatus.OPEN, null);
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 9, 24, 10, 0);
        when(alerts.findById(4L)).thenReturn(Optional.of(alert));
        when(employees.findById(2L)).thenReturn(Optional.of(employee(2L)));
        when(shelves.findById(5L)).thenReturn(Optional.of(shelf(5L)));
        when(products.findById(3L)).thenReturn(Optional.of(product(3L)));
        when(alerts.save(alert)).thenReturn(alert);

        var response = service.update(4L, new AlertUpdateRequest(LocalDate.of(2026, 9, 23),
                2L, 5L, 3L, "  Refilled  ", AlertStatus.RESOLVED, resolvedAt)).orElseThrow();

        assertThat(response.employeeId()).isEqualTo(2L);
        assertThat(response.shelfId()).isEqualTo(5L);
        assertThat(response.productId()).isEqualTo(3L);
        assertThat(response.description()).isEqualTo("Refilled");
        assertThat(response.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(response.resolutionDate()).isEqualTo(resolvedAt);
    }

    @Test
    void findByShelfRejectsUnknownShelf() {
        assertThatThrownBy(() -> service.findByShelfId(99L))
                .isInstanceOf(ShelfNotFoundException.class);
    }

    @Test
    void findByShelfReturnsOnlyMatchingAlerts() {
        Shelf shelf = shelf(1L);
        Alert alert = new Alert(4L, null, null, shelf, null, "Low stock", AlertStatus.OPEN, null);
        when(shelves.findById(1L)).thenReturn(Optional.of(shelf));
        when(alerts.findByShelfId(1L)).thenReturn(List.of(alert));

        assertThat(service.findByShelfId(1L)).extracting("id").containsExactly(4L);
    }

    @Test
    void saveRejectsUnknownOptionalReferences() {
        when(shelves.findById(1L)).thenReturn(Optional.of(shelf(1L)));
        assertThatThrownBy(() -> service.save(new AlertRequest(null, 99L, 1L, null,
                "Low stock", AlertStatus.OPEN, null)))
                .isInstanceOf(EmployeeNotFoundException.class);

        when(employees.findById(2L)).thenReturn(Optional.of(employee(2L)));
        assertThatThrownBy(() -> service.save(new AlertRequest(null, 2L, 1L, 99L,
                "Low stock", AlertStatus.OPEN, null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteRejectsUnknownAlert() {
        assertThatThrownBy(() -> service.deleteById(99L))
                .isInstanceOf(AlertNotFoundException.class);
    }

    @Test
    void deleteExistingAlert() {
        when(alerts.findById(4L)).thenReturn(Optional.of(new Alert()));

        assertThat(service.deleteById(4L)).isTrue();
        verify(alerts).deleteById(4L);
    }

    private Shelf shelf(Long id) {
        Shelf shelf = new Shelf();
        shelf.setId(id);
        return shelf;
    }

    private Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        return employee;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }
}
