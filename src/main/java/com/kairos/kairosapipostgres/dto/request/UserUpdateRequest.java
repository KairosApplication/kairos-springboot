package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Email;
import org.hibernate.validator.constraints.br.CPF;

public record UserUpdateRequest (
    @Email(message = "E-mail inválido")
    String email,
    @CPF(message = "CPF inválido")
    String cpf
){}
