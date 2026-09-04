package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRunCustomerCrudFlow() throws Exception {
        User savedUser = userRepository.save(customerUser());

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(savedUser.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(savedUser.getId()))
                .andExpect(jsonPath("$.userName").value("Davi"));

        Long customerId = customerRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/api/v1/customers").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(customerId));

        mockMvc.perform(get("/api/v1/customers/{id}", customerId).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId));

        mockMvc.perform(delete("/api/v1/customers/{id}", customerId).with(user("tester")))
                .andExpect(status().isNoContent());
        assertThat(customerRepository.existsById(customerId)).isFalse();
        assertThat(userRepository.existsById(savedUser.getId())).isTrue();
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void shouldReturnConflictWhenUserAlreadyHasCustomer() throws Exception {
        User savedUser = userRepository.save(customerUser());
        String request = request(savedUser.getId());

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Customer already exists for this user"));
    }

    @Test
    void shouldRejectInvalidCustomerRequest() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .with(user("tester"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.userId").value("O usuário é obrigatório"));
    }

    @Test
    void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", 999L).with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    private String request(Long userId) {
        return """
                {
                  "userId": %d
                }
                """.formatted(userId);
    }

    private User customerUser() {
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
