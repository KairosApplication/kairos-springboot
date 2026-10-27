package com.kairos.kairosapipostgres.dto.response;

public record RegistrationResponse(Long id, String name, String email,
                                   String accessToken, String tokenType, Long expiresIn) {
    public static RegistrationResponse of(UserResponse user, TokenResponse tokens) {
        return new RegistrationResponse(user.id(), user.name(), user.email(),
                tokens == null ? null : tokens.accessToken(), tokens == null ? null : tokens.tokenType(),
                tokens == null ? null : tokens.expiresIn());
    }
}
