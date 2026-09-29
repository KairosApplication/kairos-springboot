package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.response.EmployeeAisleResponse;
import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.EmployeeAisle;

public final class EmployeeAisleMapper {
    private EmployeeAisleMapper() {
    }

    public static EmployeeAisle toEntity(Employee employee, Aisle aisle) {
        return new EmployeeAisle(null, employee, aisle);
    }

    public static EmployeeAisleResponse toResponse(EmployeeAisle assignment) {
        return new EmployeeAisleResponse(assignment.getId(), assignment.getEmployee().getId(),
                assignment.getAisle().getId());
    }
}
