package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerBootstrapTest {
    private final UserService users = mock(UserService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);

    @Test
    void skipsWhenNotConfigured() {
        bootstrap("").run(null);
        verify(users, never()).save(any());
    }

    @Test
    void skipsWhenManagerAlreadyExists() {
        when(employees.existsByPosition(Position.MANAGER)).thenReturn(true);
        bootstrap("manager@example.com").run(null);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsAnExistingAccountInsteadOfPromotingIt() {
        when(userRepository.existsByEmail("manager@example.com")).thenReturn(true);
        assertThatThrownBy(() -> bootstrap("manager@example.com").run(null))
                .isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }

    @Test
    void createsTheFirstManager() {
        when(users.save(any(UserRequest.class)))
                .thenReturn(new UserResponse(7L, "Maria", "manager@example.com"));
        when(userRepository.getReferenceById(7L)).thenReturn(new User());
        bootstrap("manager@example.com").run(null);
        verify(users).save(any(UserRequest.class));
        verify(employees).save(any(Employee.class));
    }

    private ManagerBootstrap bootstrap(String email) {
        return new ManagerBootstrap(users, userRepository, employees, email,
                "strong-password", "52998224725", "Maria", "Silva",
                "1990-01-01", "01000-000");
    }
}
