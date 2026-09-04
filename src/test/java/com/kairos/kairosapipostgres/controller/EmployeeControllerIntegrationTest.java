package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        employeeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRunEmployeeCrudFlow() throws Exception {
        User savedUser = userRepository.save(employeeUser());

        mockMvc.perform(post("/api/v1/employees")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(savedUser.getId(), "MANAGER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.position").value("manager"))
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.userName").value("Davi"));

        Long employeeId = employeeRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/api/v1/employees/{id}", employeeId).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId));

        mockMvc.perform(patch("/api/v1/employees/{id}", employeeId)
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"position": "CASHIER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("cashier"));

        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId).with(user("tester")))
                .andExpect(status().isNoContent());
        assertThat(employeeRepository.existsById(employeeId)).isFalse();
        assertThat(userRepository.existsById(savedUser.getId())).isTrue();
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(999L, "STOCKER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void shouldReturnConflictWhenUserAlreadyHasEmployee() throws Exception {
        User savedUser = userRepository.save(employeeUser());
        String request = request(savedUser.getId(), "MANAGER");

        mockMvc.perform(post("/api/v1/employees")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/employees")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee already exists for this user"));
    }

    @Test
    void shouldRejectInvalidEmployeeRequest() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.userId").value("O usuário é obrigatório"))
                .andExpect(jsonPath("$.validationErrors.position").value("O cargo é obrigatório"));
    }

    private String request(Long userId, String position) {
        return """
                {
                  "userId": %d,
                  "position": "%s"
                }
                """.formatted(userId, position);
    }

    private User employeeUser() {
        return new User(
                null,
                "Davi",
                "Dias",
                LocalDate.of(2000, 2, 12),
                "52998224725",
                "dias@example.com",
                "encoded-password",
                "01310-100",
                Plan.STANDART
        );
    }
}
