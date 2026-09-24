package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.repository.AisleRepository;
import com.kairos.kairosapipostgres.repository.PromotionRepository;
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
class PromotionControllerIntegrationTest {
    private static final String BASE = "/api/v1/promotions";

    @Autowired private MockMvc mvc;
    @Autowired private SectorRepository sectors;
    @Autowired private AisleRepository aisles;
    @Autowired private ShelfRepository shelves;
    @Autowired private PromotionRepository promotions;

    @Test
    void shouldCreateUpdateAndRemoveShelfPromotion() throws Exception {
        Sector sector = sectors.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        Aisle aisle = aisles.saveAndFlush(new Aisle(null, sector, 1));
        Shelf shelf = shelves.saveAndFlush(new Shelf(null, aisle, 100));
        String body = """
                {"shelfId":%d,"promotionStartDate":"2026-09-24T10:00:00",
                 "promotionEndDate":"2026-09-25T10:00:00","discountPercentage":15.50}
                """.formatted(shelf.getId());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.shelfId").value(shelf.getId()))
                .andExpect(jsonPath("$.productId").isEmpty());
        Long id = promotions.findAll().getFirst().getId();
        mvc.perform(get(BASE + "/find/shelf").param("shelfId", shelf.getId().toString())
                .with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"promotionEndDate\":\"2026-09-23T10:00:00\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"discountPercentage\":20.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountPercentage").value(20.00));
        mvc.perform(delete(BASE + "/delete/{id}", id).with(user("manager").roles("MANAGER")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(promotions.count()).isZero();
    }

    @Test
    void shouldRejectInvalidPeriodAndDiscount() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"promotionStartDate":"2026-09-25T10:00:00",
                         "promotionEndDate":"2026-09-24T10:00:00"}
                        """))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"promotionStartDate":"2026-09-24T10:00:00",
                         "promotionEndDate":"2026-09-25T10:00:00","discountPercentage":101}
                        """))
                .andExpect(status().isBadRequest());
        assertThat(promotions.count()).isZero();
    }

    @Test
    void shouldReturnNotFoundForMissingPromotionAndShelf() throws Exception {
        mvc.perform(get(BASE + "/find/{id}", 999L).with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isNotFound());
        mvc.perform(post(BASE + "/registration").with(user("manager").roles("MANAGER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"shelfId":999,"promotionStartDate":"2026-09-24T10:00:00",
                         "promotionEndDate":"2026-09-25T10:00:00"}
                        """))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/list").with(user("stocker").roles("EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
