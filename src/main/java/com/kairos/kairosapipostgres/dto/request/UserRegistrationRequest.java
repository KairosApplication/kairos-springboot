package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record UserRegistrationRequest(
        @NotBlank(message = "O nome é obrigatório") String name,
        @NotBlank(message = "O sobrenome é obrigatório") String lastName,
        @NotNull(message = "A data de nascimento é obrigatória") LocalDate birthDate,
        @NotBlank(message = "A senha é obrigatória") String password,
        @NotBlank(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String email,
        @NotBlank(message = "O CPF é obrigatório") @CPF(message = "CPF inválido") String cpf,
        @NotNull(message = "A empresa é obrigatória")
        @Positive(message = "O ID da empresa deve ser positivo") Long companyId
) {
}
