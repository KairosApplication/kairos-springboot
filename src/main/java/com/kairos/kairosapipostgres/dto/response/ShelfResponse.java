package com.kairos.kairosapipostgres.dto.response;

public record ShelfResponse(Long id, Long aisleId, Integer aislePosition, Integer maximumCapacity) {
}
