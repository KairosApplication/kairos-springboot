package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountProfileServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final AccountProfileService service = new AccountProfileService(users, employees, customers);
    private final User user = new User(42L, "Ana", "Silva", LocalDate.of(2000, 1, 1),
            "52998224725", "own@example.invalid", "not-a-login");
    private final Company company = new Company(57L, "Mercado de teste", "00000000000001", "market@invalid", "BASIC");
    private Authentication auth(String... roles) {
        return new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                java.util.Arrays.stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
    }
    @Test
    void customerGetsOnlyOwnPersonalDataAndMarket() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(customers.findByUserId(42L)).thenReturn(Optional.of(new Customer(12L, user, company)));
        var result = service.current(auth("CUSTOMER"));
        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.name()).isEqualTo("Ana");
        assertThat(result.lastName()).isEqualTo("Silva");
        assertThat(result.birthDate()).isEqualTo(user.getBirthDate());
        assertThat(result.cpf()).isEqualTo(user.getCpf());
        assertThat(result.companyId()).isEqualTo(57L);
        assertThat(result.companyName()).isEqualTo(company.getName());
        assertThat(result.customerId()).isEqualTo(12L);
        assertThat(result.employeeId()).isNull();
        verifyNoInteractions(employees);
    }
    @Test
    void stockerAndManagerGetTheirEmployeeLink() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        for (Position position : List.of(Position.STOCKER, Position.MANAGER)) {
            when(employees.findByUserId(42L)).thenReturn(Optional.of(new Employee(157L, position, user, company)));
            var result = service.current(auth(position == Position.MANAGER ? "MANAGER" : "EMPLOYEE"));
            assertThat(result.employeeId()).isEqualTo(157L);
            assertThat(result.position()).isEqualTo(position.name());
            assertThat(result.companyName()).isEqualTo(company.getName());
            assertThat(result.customerId()).isNull();
        }
        verifyNoInteractions(customers);
    }
    @Test
    void missingOptionalFieldsAreNotInvented() {
        user.setBirthDate(null);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(employees.findByUserId(42L)).thenReturn(Optional.of(new Employee(157L, null, user, null)));
        var result = service.current(auth("EMPLOYEE"));
        assertThat(result.birthDate()).isNull();
        assertThat(result.companyId()).isNull();
        assertThat(result.companyName()).isNull();
        assertThat(result.position()).isNull();
    }
    @Test
    void deniesUnauthenticatedAndUnknownAccounts() {
        assertThatThrownBy(() -> service.current(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.current(new UsernamePasswordAuthenticationToken("unknown", null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.current(auth("CUSTOMER"))).isInstanceOf(AccessDeniedException.class);
    }
    @Test
    void deniesMissingLinksAndUnrecognizedRoles() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.current(auth("EMPLOYEE"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.current(auth("CUSTOMER"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.current(auth("OTHER"))).isInstanceOf(AccessDeniedException.class);
    }
}
