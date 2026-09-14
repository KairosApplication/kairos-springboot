package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldExposeHealthyApplicationWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldRegisterUserWithoutAuthenticationAndEncodePassword() throws Exception {
        mockMvc.perform(post("/api/v1/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.cpf").value("529.982.247-25"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = repository.findByEmail("ana@example.com").orElseThrow();
        assertThat(saved.getLastName()).isEqualTo("Silva");
        assertThat(saved.getBirthDate()).hasToString("1995-05-20");
        assertThat(saved.getZipCode()).isEqualTo("01310-100");
        assertThat(passwordEncoder.matches("secret", saved.getPassword())).isTrue();
    }

    @Test
    void shouldRejectInvalidRegistrationWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.validationErrors.name").value("O nome é obrigatório"))
                .andExpect(jsonPath("$.validationErrors.email").value("O e-mail é obrigatório"));
    }

    @Test
    void shouldReturnConflictForDuplicateUser() throws Exception {
        mockMvc.perform(post("/api/v1/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    void shouldRequireAuthenticationToListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRunAuthenticatedFindUpdateAndDeleteFlow() throws Exception {
        mockMvc.perform(post("/api/v1/users/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistration()))
                .andExpect(status().isCreated());
        Long id = repository.findByEmail("ana@example.com").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users/find/{id}", id).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(patch("/api/v1/users/update/{id}", id)
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "plan": "CORPORATIVO",
                                  "email": "new@example.com",
                                  "cpf": "111.444.777-35"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.cpf").value("111.444.777-35"));

        mockMvc.perform(delete("/api/v1/users/delete/{id}", id).with(user("tester")))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void shouldReturnNotFoundForMissingUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/find/{id}", 999L).with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    private String validRegistration() {
        return """
                {
                  "name": "Ana",
                  "lastName": "Silva",
                  "birthDate": "1995-05-20",
                  "password": "secret",
                  "zipCode": "01310-100",
                  "plan": "STANDART",
                  "email": "ana@example.com",
                  "cpf": "529.982.247-25"
                }
                """;
    }
}
