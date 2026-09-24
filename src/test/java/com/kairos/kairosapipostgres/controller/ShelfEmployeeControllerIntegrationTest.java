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
class ShelfEmployeeControllerIntegrationTest {
    private static final String BASE = "/api/v1/shelf-employees";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private ShelfEmployeeRepository assignments;

    @Test
    void shouldAssignMoveAndRemoveEmployeeFromShelf() throws Exception {
        User user = users.saveAndFlush(new User(null, "Davi", "Dias", LocalDate.of(2000, 2, 12),
                "52998224725", "davi@example.com", "hash", "01310-100", Plan.STANDART));
        Employee employee = employees.saveAndFlush(new Employee(null, Position.STOCKER, user));
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle aisle = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Shelf first = shelves.saveAndFlush(new Shelf(null, aisle, 100));
        Shelf second = shelves.saveAndFlush(new Shelf(null, aisle, 200));
        String body = request(employee.getId(), first.getId());

        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.shelfId").value(first.getId()));
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        ShelfEmployee assignment = assignments.findByEmployeeIdAndShelfId(employee.getId(), first.getId()).orElseThrow();
        mvc.perform(get(BASE + "/find/shelf").param("shelfId", first.getId().toString())
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch(BASE + "/update/{id}", assignment.getId())
                .with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shelfId\":%d}".formatted(second.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.shelfId").value(second.getId()));
        mvc.perform(delete(BASE + "/delete/{id}", assignment.getId())
                .with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(assignments.count()).isZero();
    }

    private String request(Long employeeId, Long shelfId) {
        return "{\"employeeId\":%d,\"shelfId\":%d}".formatted(employeeId, shelfId);
    }
}
