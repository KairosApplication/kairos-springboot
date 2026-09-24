package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.repository.AisleRepository;
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
class ShelfControllerIntegrationTest {
    private static final String BASE = "/api/v1/shelves";

    @Autowired private MockMvc mvc;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;

    @Test
    void shouldCreateFindUpdateAndDeleteShelf() throws Exception {
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle first = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Aisle second = aisles.saveAndFlush(new Aisle(null, sector, 2));

        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aisleId\":%d,\"maximumCapacity\":100}".formatted(first.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.maximumCapacity").value(100));
        Shelf shelf = shelves.findAll().getFirst();
        mvc.perform(get(BASE + "/find/{id}", shelf.getId()).with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aisleId").value(first.getId()));
        mvc.perform(get(BASE + "/find/aisle").param("aisleId", first.getId().toString())
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        mvc.perform(patch(BASE + "/update/{id}", shelf.getId())
                .with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aisleId\":%d,\"maximumCapacity\":200}".formatted(second.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aisleId").value(second.getId()))
                .andExpect(jsonPath("$.maximumCapacity").value(200));
        assertThat(shelves.findByAisleId(second.getId())).hasSize(1);

        mvc.perform(delete(BASE + "/delete/{id}", shelf.getId())
                .with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(shelves.existsById(shelf.getId())).isFalse();
    }

    @Test
    void shouldRejectInvalidCapacityAndMissingAisle() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aisleId\":999999,\"maximumCapacity\":10}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aisleId\":1,\"maximumCapacity\":0}"))
                .andExpect(status().isBadRequest());
        assertThat(shelves.count()).isZero();
    }

    @Test
    void shouldRestrictShelfWritesToManagers() throws Exception {
        mvc.perform(get(BASE + "/list")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/list").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/registration").with(user("stocker").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
