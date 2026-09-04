package com.kairos.kairosapipostgres.dto.response;

public record EmployeeResponse(
        Long id,
        String position,
        Long userId,
        String userName
) {
}
