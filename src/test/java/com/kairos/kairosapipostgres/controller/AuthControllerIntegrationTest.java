package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.Employee;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.EmployeeRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    private static final String PASSWORD = "test-password-123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private EmployeeRepository employees;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void shouldLoginWithDatabaseCredentialsRotateSessionAndPreserveAuthentication() throws Exception {
        User user = createUser("customer@example.com");
        customers.saveAndFlush(new Customer(null, user));
        CsrfSession csrf = csrf(null);
        String originalId = csrf.session().getId();

        mvc.perform(post("/api/v1/auth/login").session(csrf.session())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", user.getEmail()).param("password", PASSWORD)
                .param("roles", "MANAGER"))
                .andExpect(status().isNoContent());

        assertThat(csrf.session().getId()).isNotEqualTo(originalId);
        mvc.perform(get("/api/v1/auth/me").session(csrf.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/v1/products/list").session(csrf.session()))
                .andExpect(status().isOk());
        CsrfSession refreshed = csrf(csrf.session());
        mvc.perform(post("/api/v1/employees/registration").session(refreshed.session())
                .header(refreshed.headerName(), refreshed.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(employeeRequest("newhire@example.com")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer@example.com", "unknown@example.com", "unassigned@example.com"})
    void shouldRejectWrongCredentialsOrAccountWithoutProfile(String email) throws Exception {
        User customer = createUser("customer@example.com");
        customers.saveAndFlush(new Customer(null, customer));
        createUser("unassigned@example.com");
        CsrfSession csrf = csrf(null);
        String password = email.equals("customer@example.com") ? "wrong-password" : PASSWORD;

        mvc.perform(post("/api/v1/auth/login").session(csrf.session())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", email).param("password", password))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        mvc.perform(get("/api/v1/auth/me").session(csrf.session()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireCsrfForLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "customer@example.com").param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAuthorizeManagerToRegisterEmployeeButNotReadProducts() throws Exception {
        User manager = createUser("manager@example.com");
        employees.saveAndFlush(new Employee(null, Position.MANAGER, manager));
        CsrfSession session = login(manager.getEmail());

        mvc.perform(get("/api/v1/products/list").session(session.session()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/employees/registration").session(session.session())
                .header(session.headerName(), session.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(employeeRequest("employee@example.com")))
                .andExpect(status().isCreated());
        assertThat(users.findByEmail("employee@example.com")).isPresent();
    }

    @Test
    void shouldKeepRegularEmployeeRoleWithoutGrantingCustomerAccess() throws Exception {
        User employee = createUser("employee@example.com");
        employees.saveAndFlush(new Employee(null, Position.CASHIER, employee));
        CsrfSession session = login(employee.getEmail());
        mvc.perform(get("/api/v1/auth/me").session(session.session()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));
        mvc.perform(get("/api/v1/products/list").session(session.session()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRotateCsrfOnLoginAndInvalidateSessionOnLogout() throws Exception {
        User customer = createUser("customer@example.com");
        customers.saveAndFlush(new Customer(null, customer));
        CsrfSession before = csrf(null);
        mvc.perform(post("/api/v1/auth/login").session(before.session())
                .header(before.headerName(), before.token())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", customer.getEmail()).param("password", PASSWORD))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/logout").session(before.session())
                .header(before.headerName(), before.token()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/logout").session(before.session()))
                .andExpect(status().isForbidden());

        CsrfSession after = csrf(before.session());
        mvc.perform(post("/api/v1/auth/logout").session(after.session())
                .header(after.headerName(), after.token()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertThat(after.session().isInvalid()).isTrue();
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAnonymousAccessAndBasicAuthentication() throws Exception {
        User customer = createUser("customer@example.com");
        customers.saveAndFlush(new Customer(null, customer));
        mvc.perform(get("/api/v1/products/list"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/products/list").with(httpBasic(customer.getEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized());
        CsrfSession csrf = csrf(null);
        mvc.perform(post("/api/v1/auth/logout").session(csrf.session())
                .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowLoginPreflightOnlyFromConfiguredFrontend() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/v1/auth/login")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    private CsrfSession login(String email) throws Exception {
        CsrfSession csrf = csrf(null);
        mvc.perform(post("/api/v1/auth/login").session(csrf.session())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", email).param("password", PASSWORD))
                .andExpect(status().isNoContent());
        return csrf(csrf.session());
    }

    private CsrfSession csrf(MockHttpSession session) throws Exception {
        var request = get("/api/v1/auth/login");
        if (session != null) {
            request.session(session);
        }
        MvcResult result = mvc.perform(request)
                .andExpect(status().isOk()).andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfSession((MockHttpSession) result.getRequest().getSession(false),
                body.get("headerName").asText(), body.get("token").asText());
    }

    private User createUser(String email) {
        return users.saveAndFlush(new User(null, "Ana", "Silva", LocalDate.of(2000, 1, 1),
                "52998224725", email, passwordEncoder.encode(PASSWORD), "01310-100", Plan.STANDART));
    }

    private String employeeRequest(String email) {
        return """
                {
                  "user": {
                    "name": "Davi",
                    "lastName": "Dias",
                    "birthDate": "2000-02-12",
                    "password": "password-123",
                    "zipCode": "01310-100",
                    "plan": "STANDART",
                    "email": "%s",
                    "cpf": "11144477735"
                  },
                  "position": "CASHIER"
                }
                """.formatted(email);
    }

    private record CsrfSession(MockHttpSession session, String headerName, String token) {
    }
}
