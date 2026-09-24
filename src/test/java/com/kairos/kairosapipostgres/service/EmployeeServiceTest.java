package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.request.EmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.EmployeeUpdateRequest;
import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.exception.EmployeeNotFoundException;
import com.kairos.kairosapipostgres.exception.InvalidEmployeePositionException;
import com.kairos.kairosapipostgres.exception.UserAlreadyExistsException;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(employeeRepository, userRepository, userService);
    }

    @Test
    void shouldCreateUserAndEmployee() {
        UserRequest userRequest = userRequest();
        User user = user(10L);
        when(userService.save(userRequest)).thenReturn(new UserResponse(10L, "Davi", "dias@example.com"));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        EmployeeResponse response = service.save(new EmployeeRequest(userRequest, Position.CASHIER));

        assertThat(response).isEqualTo(new EmployeeResponse(
                1L,
                "cashier",
                10L,
                "Davi"
        ));
        verify(userService).save(userRequest);
        verify(employeeRepository).save(argThat(saved ->
                saved.getPosition() == Position.CASHIER && saved.getUser() == user));
    }

    @Test
    void shouldRejectManagerRegistrationBeforeCreatingUser() {
        assertThatThrownBy(() -> service.save(new EmployeeRequest(userRequest(), Position.MANAGER)))
                .isInstanceOf(InvalidEmployeePositionException.class)
                .hasMessage("The employee position must be CASHIER or STOCKER");
        verify(userService, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldNotCreateEmployeeWhenUserAlreadyExists() {
        UserRequest userRequest = userRequest();
        when(userService.save(userRequest)).thenThrow(new UserAlreadyExistsException("User already exists"));

        assertThatThrownBy(() -> service.save(new EmployeeRequest(userRequest, Position.STOCKER)))
                .isInstanceOf(UserAlreadyExistsException.class);
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

        EmployeeResponse response = service.findById(1L).orElseThrow();

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
                new EmployeeUpdateRequest(Position.STOCKER)
        ).orElseThrow();

        assertThat(employee.getPosition()).isEqualTo(Position.STOCKER);
        assertThat(response.position()).isEqualTo("stocker");
        verify(employeeRepository).save(employee);
    }

    @Test
    void shouldRejectManagerPromotionWithoutChangingEmployee() {
        assertThatThrownBy(() -> service.update(1L, new EmployeeUpdateRequest(Position.MANAGER)))
                .isInstanceOf(InvalidEmployeePositionException.class)
                .hasMessage("The employee position must be CASHIER or STOCKER");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingEmployee() {
        Employee employee = employee(1L, Position.CASHIER);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        service.deleteById(1L);

        verify(employeeRepository).deleteById(1L);
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

    private UserRequest userRequest() {
        return new UserRequest("Davi", "Dias", LocalDate.of(2000, 2, 12),
                "password", "01310-100", Plan.STANDART, "dias@example.com", "52998224725");
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
