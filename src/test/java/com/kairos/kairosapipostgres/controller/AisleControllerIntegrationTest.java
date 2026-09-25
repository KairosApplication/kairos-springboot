package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.repository.AisleRepository;
import com.kairos.kairosapipostgres.repository.SectorRepository;
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
class AisleControllerIntegrationTest {
    private static final String BASE = "/api/v1/aisles";

    @Autowired private MockMvc mvc;
    @Autowired private AisleRepository aisles;
    @Autowired private SectorRepository sectors;

    @Test
    void shouldCreateListMoveAndDeleteAisle() throws Exception {
        Sector first = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Sector second = sectors.saveAndFlush(new Sector(null, "Vendas", "Operacional"));

        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":%d,\"position\":1}".formatted(first.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.position").value(1));
        Aisle aisle = aisles.findBySectorIdAndPosition(first.getId(), 1).orElseThrow();
        aisles.saveAndFlush(new Aisle(null, first, 2));

        mvc.perform(get(BASE + "/find/sector").param("sectorId", first.getId().toString())
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[1].position").value(2));
        mvc.perform(get(BASE + "/find/{id}", aisle.getId()).with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sectorId").value(first.getId()));
        mvc.perform(get(BASE + "/list").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        mvc.perform(patch(BASE + "/update/{id}", aisle.getId())
                .with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":%d,\"position\":3}".formatted(second.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sectorId").value(second.getId()))
                .andExpect(jsonPath("$.position").value(3));
        assertThat(aisles.findBySectorIdAndPosition(second.getId(), 3)).isPresent();

        mvc.perform(delete(BASE + "/delete/{id}", aisle.getId())
                .with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(aisles.existsById(aisle.getId())).isFalse();
    }

    @Test
    void shouldRejectDuplicatePositionAndInvalidReferences() throws Exception {
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle first = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Aisle second = aisles.saveAndFlush(new Aisle(null, sector, 2));

        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":%d,\"position\":1}".formatted(sector.getId())))
                .andExpect(status().isConflict());
        mvc.perform(patch(BASE + "/update/{id}", second.getId())
                .with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":1}"))
                .andExpect(status().isConflict());
        assertThat(aisles.findById(second.getId()).orElseThrow().getPosition()).isEqualTo(2);
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":999999,\"position\":3}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectorId\":%d,\"position\":0}".formatted(sector.getId())))
                .andExpect(status().isBadRequest());
        assertThat(aisles.findById(first.getId())).isPresent();
    }

    @Test
    void shouldEnforceWarehouseRoles() throws Exception {
        mvc.perform(get(BASE + "/list")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/list").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/registration").with(user("stocker").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
