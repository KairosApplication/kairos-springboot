package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.EmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.EmployeeUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.exception.EmployeeAlreadyExistsException;
import com.kairos.kairosapipostgres.exception.EmployeeNotFoundException;
import com.kairos.kairosapipostgres.exception.UserNotFoundException;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(employeeRepository, userRepository);
    }

    @Test
    void shouldCreateEmployeeForExistingUser() {
        User user = user(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(employeeRepository.existsByUserId(10L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        EmployeeResponse response = service.save(new EmployeeRequest(10L, Position.MANAGER));

        assertThat(response).isEqualTo(new EmployeeResponse(
                1L,
                "manager",
                10L,
                "Davi"
        ));
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void shouldRejectEmployeeWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(new EmployeeRequest(99L, Position.STOCKER)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldRejectSecondEmployeeForSameUser() {
        User user = user(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(employeeRepository.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.save(new EmployeeRequest(10L, Position.CASHIER)))
                .isInstanceOf(EmployeeAlreadyExistsException.class)
                .hasMessage("Employee already exists for this user");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldListEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of(
                employee(1L, Position.MANAGER),
                employee(2L, Position.CASHIER)
        ));

        List<EmployeeResponse> employees = service.findAll();

        assertThat(employees).hasSize(2);
        assertThat(employees).extracting(EmployeeResponse::position)
                .containsExactly("manager", "cashier");
    }

    @Test
    void shouldFindEmployeeById() {
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee(1L, Position.STOCKER)));

        EmployeeResponse response = service.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.position()).isEqualTo("stocker");
    }

    @Test
    void shouldUpdateEmployeePosition() {
        Employee employee = employee(1L, Position.CASHIER);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        EmployeeResponse response = service.update(
                1L,
                new EmployeeUpdateRequest(Position.MANAGER)
        );

        assertThat(employee.getPosition()).isEqualTo(Position.MANAGER);
        assertThat(response.position()).isEqualTo("manager");
        verify(employeeRepository).save(employee);
    }

    @Test
    void shouldDeleteExistingEmployee() {
        Employee employee = employee(1L, Position.CASHIER);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        service.deleteById(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void shouldRejectMissingEmployee() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessage("Employee not found");
    }

    private Employee employee(Long id, Position position) {
        return new Employee(id, position, user(10L));
    }

    private User user(Long id) {
        return new User(
                id,
                "Davi",
                "Dias",
                LocalDate.of(2000, 2, 12),
                "52998224725",
                "dias@example.com",
                "encoded-password",
                "01310-100",
                Plan.STANDART
        );
    }
}
