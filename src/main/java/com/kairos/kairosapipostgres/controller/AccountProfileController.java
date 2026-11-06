package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.response.AccountProfileResponse;
import com.kairos.kairosapipostgres.service.AccountProfileService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/profile")
public class AccountProfileController {
    private final AccountProfileService profiles;

    public AccountProfileController(AccountProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public ResponseEntity<AccountProfileResponse> current(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profiles.current(auth));
    }
}
