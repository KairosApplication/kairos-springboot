package com.kairos.kairosapipostgres.dto.response;

import java.time.LocalDate;

public record AccountProfileResponse(Long userId, String email, String name, String lastName,
        LocalDate birthDate, String cpf, Long employeeId, Long customerId,
        Long companyId, String companyName, String position) {
}
