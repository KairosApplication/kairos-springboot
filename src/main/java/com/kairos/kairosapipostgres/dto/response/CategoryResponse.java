package com.kairos.kairosapipostgres.dto.response;

public record CategoryResponse(
        Long id,
        String category
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
}
