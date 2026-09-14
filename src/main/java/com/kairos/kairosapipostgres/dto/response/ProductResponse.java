package com.kairos.kairosapipostgres.dto.response;


import java.math.BigDecimal;

public record ProductResponse (
        Long id,
        String brand,
        BigDecimal price,
        String name,
        CategoryResponse category
){
}
