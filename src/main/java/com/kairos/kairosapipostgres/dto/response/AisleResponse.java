package com.kairos.kairosapipostgres.dto.response;

public record AisleResponse (
        Long id,
        Long sectorId,
        String sectorName,
        Integer position
) {
}
