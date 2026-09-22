package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.EmployeeRequest;
import com.kairos.kairosapipostgres.dto.request.UserRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.model.enums.Position;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeMapperTest {

    @Test
    void shouldMapRequestAndUserToEntity() {
        User user = user(10L);
        UserRequest userRequest = new UserRequest("Davi", "Dias", LocalDate.of(2000, 2, 12),
                "password", "01310-100", Plan.STANDART, "dias@example.com", "52998224725");
        EmployeeRequest request = new EmployeeRequest(userRequest, Position.CASHIER);

        Employee employee = EmployeeMapper.toEntity(request, user);

        assertThat(employee.getId()).isNull();
        assertThat(employee.getPosition()).isEqualTo(Position.CASHIER);
        assertThat(employee.getUser()).isSameAs(user);
    }

    @Test
    void shouldMapEntityToResponse() {
        Employee employee = new Employee(7L, Position.CASHIER, user(10L));

        EmployeeResponse response = EmployeeMapper.toResponse(employee);

        assertThat(response).isEqualTo(new EmployeeResponse(
                7L,
                "cashier",
                10L,
                "Davi"
        ));
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
