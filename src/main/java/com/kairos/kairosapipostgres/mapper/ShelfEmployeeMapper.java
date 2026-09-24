package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.response.ShelfEmployeeResponse;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.ShelfEmployee;

public final class ShelfEmployeeMapper {
    private ShelfEmployeeMapper() {
    }

    public static ShelfEmployee toEntity(Employee employee, Shelf shelf) {
        return new ShelfEmployee(null, employee, shelf);
    }

    public static ShelfEmployeeResponse toResponse(ShelfEmployee assignment) {
        return new ShelfEmployeeResponse(assignment.getId(), assignment.getEmployee().getId(),
                assignment.getShelf().getId());
    }
}
