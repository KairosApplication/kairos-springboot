package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductUpdateRequest(
        @Positive(message = "A categoria deve ter um ID positivo")
        Long categoryId,
        @Pattern(regexp = "(?s).*\\S.*", message = "A marca não pode estar em branco")
        @Size(max = 255)
        String brand,
        @DecimalMin(value = "0.0", message = "O preço não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "O preço deve ter até 10 dígitos inteiros e 2 decimais")
        BigDecimal price,
        @Pattern(regexp = "(?s).*\\S.*", message = "O nome não pode estar em branco")
        @Size(max = 100)
        String name
) {
}
