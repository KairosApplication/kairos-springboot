package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Plan;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeAisleControllerIntegrationTest {
    private static final String BASE = "/api/v1/employee-aisles";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private EmployeeAisleRepository assignments;

    @Test
    void shouldAssignUpdateAndRemoveEmployeeFromAisle() throws Exception {
        Employee employee = employee();
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle first = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Aisle second = aisles.saveAndFlush(new Aisle(null, sector, 2));
        String firstRequest = request(employee.getId(), first.getId());

        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(firstRequest))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeId").value(employee.getId()));
        EmployeeAisle assignment = assignments.findByEmployeeIdAndAisleId(employee.getId(), first.getId()).orElseThrow();
        mvc.perform(get(BASE + "/find/employee").param("employeeId", employee.getId().toString())
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        mvc.perform(patch(BASE + "/update/{id}", assignment.getId())
                .with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aisleId\":%d}".formatted(second.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aisleId").value(second.getId()));
        mvc.perform(get(BASE + "/find/aisle").param("aisleId", second.getId().toString())
                .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete(BASE + "/delete/{id}", assignment.getId())
                .with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(assignments.count()).isZero();
    }

    @Test
    void shouldRejectDuplicateAndMissingReferences() throws Exception {
        Employee employee = employee();
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle aisle = aisles.saveAndFlush(new Aisle(null, sector, 1));
        String body = request(employee.getId(), aisle.getId());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request(employee.getId(), 999999L)))
                .andExpect(status().isNotFound());
        assertThat(assignments.count()).isEqualTo(1);
    }

    private Employee employee() {
        User user = users.saveAndFlush(new User(null, "Davi", "Dias", LocalDate.of(2000, 2, 12),
                "52998224725", "davi@example.com", "hash", "01310-100", Plan.STANDART));
        return employees.saveAndFlush(new Employee(null, Position.STOCKER, user));
    }

    private String request(Long employeeId, Long aisleId) {
        return "{\"employeeId\":%d,\"aisleId\":%d}".formatted(employeeId, aisleId);
    }
}
