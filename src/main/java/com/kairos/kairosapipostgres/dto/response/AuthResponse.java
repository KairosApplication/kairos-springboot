package com.kairos.kairosapipostgres.dto.response;

import java.util.List;

public record AuthResponse(String email, List<String> roles) {

    public AuthResponse {
        roles = List.copyOf(roles);
    }

    @Override
    public List<String> roles() {
        return List.copyOf(roles);
    }
}
