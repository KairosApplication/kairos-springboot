package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest (
        @NotNull(message = "A categoria é obrigatória")
        @Positive(message = "A categoria deve ter um ID positivo")
        Long categoryId,
        @NotBlank(message = "O nome da marca é obrigatório")
        @Size(max = 255)
        String brand,
        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.0", message = "O preço não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "O preço deve ter até 10 dígitos inteiros e 2 decimais")
        BigDecimal price,
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100)
        String name
){
}
