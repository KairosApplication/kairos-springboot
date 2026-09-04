package com.kairos.kairosapipostgres.dto.request;

import com.kairos.kairosapipostgres.model.enums.Plan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record UserRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,
        @NotBlank(message = "O sobrenome é obrigatório")
        String lastName,
        @NotNull(message = "A data de nascimento é obrigatória")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,
        @NotBlank(message = "A senha é obrigatória")
        String password,
        @NotBlank(message = "O CEP é obrigatório")
        String zipCode,
        @NotNull(message = "O plano é obrigatório")
        Plan plan,
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,
        @NotBlank(message = "O CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf
){}
