package com.kairos.kairosapipostgres.dto.request;

import com.kairos.kairosapipostgres.model.enums.Plan;
import jakarta.validation.constraints.Email;
import org.hibernate.validator.constraints.br.CPF;

public record UserUpdateRequest (
    Plan plan,
    @Email(message = "E-mail inválido")
    String email,
    @CPF(message = "CPF inválido")
    String cpf
){}
