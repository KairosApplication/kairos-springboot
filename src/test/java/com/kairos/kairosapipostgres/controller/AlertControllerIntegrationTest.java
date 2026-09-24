package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.repository.AisleRepository;
import com.kairos.kairosapipostgres.repository.AlertRepository;
import com.kairos.kairosapipostgres.repository.SectorRepository;
import com.kairos.kairosapipostgres.repository.ShelfRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlertControllerIntegrationTest {
    private static final String BASE = "/api/v1/alerts";

    @Autowired private MockMvc mvc;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private AlertRepository alerts;

    @Test
    void shouldCreateResolveAndDeleteAlertWithoutOptionalReferences() throws Exception {
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle aisle = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Shelf shelf = shelves.saveAndFlush(new Shelf(null, aisle, 100));
        mvc.perform(post(BASE + "/registration").with(user("stocker").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shelfId\":%d,\"description\":\"Falta produto\",\"status\":\"OPEN\"}"
                        .formatted(shelf.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.employeeId").isEmpty());
        Long id = alerts.findAll().getFirst().getId();
        mvc.perform(get(BASE + "/find/shelf").param("shelfId", shelf.getId().toString())
                .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\",\"resolutionDate\":\"2026-09-24T10:00:00\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(delete(BASE + "/delete/{id}", id).with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(alerts.count()).isZero();
    }

    @Test
    void shouldRejectMissingShelfOrStatus() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("stocker").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Falta produto\"}"))
                .andExpect(status().isBadRequest());
    }
}
