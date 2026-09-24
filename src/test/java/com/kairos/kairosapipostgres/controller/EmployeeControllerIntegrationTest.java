package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        employeeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequireManagerForEmployeeRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/employees/list"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("CASHIER")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(user("customer").roles("CUSTOMER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("CASHIER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateUserAndRunEmployeeCrudFlow() throws Exception {
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("CASHIER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.position").value("cashier"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.userName").value("Davi"));

        var savedUser = userRepository.findByEmail("dias@example.com").orElseThrow();
        var savedEmployee = employeeRepository.findByUserId(savedUser.getId()).orElseThrow();
        assertThat(savedEmployee.getPosition()).isEqualTo(Position.CASHIER);
        assertThat(passwordEncoder.matches("password-123", savedUser.getPassword())).isTrue();

        mockMvc.perform(get("/api/v1/employees/find/{id}", savedEmployee.getId())
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedEmployee.getId()));
        mockMvc.perform(patch("/api/v1/employees/update/{id}", savedEmployee.getId())
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":\"STOCKER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("stocker"));
        mockMvc.perform(delete("/api/v1/employees/delete/{id}", savedEmployee.getId())
                        .with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(employeeRepository.existsById(savedEmployee.getId())).isFalse();
        assertThat(userRepository.existsById(savedUser.getId())).isTrue();
    }

    @Test
    void shouldReturnConflictForDuplicateUser() throws Exception {
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("STOCKER")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("STOCKER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(employeeRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectManagerPositionWithoutCreatingUser() throws Exception {
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("MANAGER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("The employee position must be CASHIER or STOCKER"));

        assertThat(userRepository.count()).isZero();
        assertThat(employeeRepository.count()).isZero();
    }

    @Test
    void shouldValidateNestedUser() throws Exception {
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.user").value("O usuário é obrigatório"))
                .andExpect(jsonPath("$.validationErrors.position").value("O cargo é obrigatório"));
        mockMvc.perform(post("/api/v1/employees/registration")
                        .with(user("manager").roles("MANAGER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("CASHIER").replace("dias@example.com", "invalid-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['validationErrors']['user.email']").value("E-mail inválido"));
    }

    private String request(String position) {
        return """
                {
                  "user": {
                    "name": "Davi",
                    "lastName": "Dias",
                    "birthDate": "2000-02-12",
                    "password": "password-123",
                    "zipCode": "01310-100",
                    "plan": "STANDART",
                    "email": "dias@example.com",
                    "cpf": "52998224725"
                  },
                  "position": "%s"
                }
                """.formatted(position);
    }
}
