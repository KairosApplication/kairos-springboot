package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.EmployeeRequest;
import com.kairos.kairosapipostgres.dto.response.EmployeeResponse;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.User;

public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static Employee toEntity(EmployeeRequest request, User user) {
        Employee employee = new Employee();
        employee.setPosition(request.position());
        employee.setUser(user);
        return employee;
    }

    public static EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getPosition().getValue(),
                employee.getUser().getId(),
                employee.getUser().getName()
        );
    }
}
