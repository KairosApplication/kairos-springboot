package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.request.UserRegistrationRequest;
import com.kairos.kairosapipostgres.dto.request.UserUpdateRequest;
import com.kairos.kairosapipostgres.dto.response.UserResponse;
import com.kairos.kairosapipostgres.service.UserService;
import com.kairos.kairosapipostgres.service.CustomerRegistrationService;
import com.kairos.kairosapipostgres.service.RegistrationSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final CustomerRegistrationService registrationService;
    private final RegistrationSessionService registrationSessionService;

    public UserController(UserService userService, CustomerRegistrationService registrationService,
                          RegistrationSessionService registrationSessionService) {
        this.userService = userService;
        this.registrationService = registrationService;
        this.registrationSessionService = registrationSessionService;
    }

    @PostMapping("/registration")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {

        UserResponse cliente = registrationService.register(request);
        if (servletRequest.getUserPrincipal() == null) {
            registrationSessionService.login(request.email(), request.password(), servletRequest, servletResponse);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cliente);
    }

    @GetMapping("/list")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("find/{id}")
    public ResponseEntity<Optional<UserResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PatchMapping("update/{id}")
    public ResponseEntity<Optional<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(
                userService.update (id, request)
        );
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteById (id);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
