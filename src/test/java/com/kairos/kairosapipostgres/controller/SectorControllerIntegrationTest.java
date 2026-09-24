package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Sector;
import com.kairos.kairosapipostgres.repository.SectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SectorControllerIntegrationTest {

    private static final String BASE = "/api/v1/sectors";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SectorRepository repository;

    @Test
    void shouldRegisterFindListAndDeleteSector() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Estoque\",\"type\":\"Operacional\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Estoque"))
                .andExpect(jsonPath("$.type").value("Operacional"));
        Long id = repository.findByName("Estoque").orElseThrow().getId();

        mvc.perform(get(BASE + "/find/{id}", id).with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(BASE + "/find/name").param("name", "Estoque").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(BASE + "/list").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete(BASE + "/delete/{id}", id).with(user("tester")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "{\"name\":\"Novo\"}|Novo|Operacional",
            "{\"type\":\"Administrativo\"}|Estoque|Administrativo",
            "{\"name\":\"Novo\",\"type\":\"Administrativo\"}|Novo|Administrativo",
            "{\"name\":\" Novo \",\"type\":\" Administrativo \"}|Novo|Administrativo"
    }, delimiter = '|')
    void shouldUpdateOnlyProvidedFields(String body, String expectedName, String expectedType) throws Exception {
        Sector sector = repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));

        mvc.perform(patch(BASE + "/update/{id}", sector.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sector.getId()))
                .andExpect(jsonPath("$.name").value(expectedName))
                .andExpect(jsonPath("$.type").value(expectedType));

        repository.flush();
        Sector updated = repository.findById(sector.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo(expectedName);
        assertThat(updated.getType()).isEqualTo(expectedType);
        assertThat(repository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null,\"type\":null}", "{\"name\":\"Estoque\"}"})
    void shouldKeepValuesForEmptyNullOrUnchangedUpdate(String body) throws Exception {
        Sector sector = repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        mvc.perform(patch(BASE + "/update/{id}", sector.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Estoque"))
                .andExpect(jsonPath("$.type").value("Operacional"));
    }

    @Test
    void shouldFindAllSectorsOfTheSameType() throws Exception {
        repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        repository.saveAndFlush(new Sector(null, "Vendas", "Operacional"));
        repository.saveAndFlush(new Sector(null, "Financeiro", "Administrativo"));

        mvc.perform(get(BASE + "/find/type").param("type", "Operacional").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.containsInAnyOrder("Estoque", "Vendas")));
        mvc.perform(get(BASE + "/find/type").param("type", "Inexistente").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Estoque", " Estoque "})
    void shouldRejectDuplicateNameOnRegistration(String name) throws Exception {
        repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        mvc.perform(post(BASE + "/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"type\":\"Outro\"}".formatted(name)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sector already exists"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Vendas", " Vendas "})
    void shouldRejectDuplicateNameWithoutChangingEitherField(String name) throws Exception {
        Sector sector = repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        repository.saveAndFlush(new Sector(null, "Vendas", "Operacional"));
        mvc.perform(patch(BASE + "/update/{id}", sector.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"type\":\"Outro\"}".formatted(name)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sector already exists"));
        Sector unchanged = repository.findById(sector.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Estoque");
        assertThat(unchanged.getType()).isEqualTo("Operacional");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\"\"}", "{\"name\":\"   \"}", "{\"type\":\"\"}", "{\"type\":\"   \"}"})
    void shouldRejectBlankUpdateFields(String body) throws Exception {
        Sector sector = repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        mvc.perform(patch(BASE + "/update/{id}", sector.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request"));
        assertThat(repository.findById(sector.getId()).orElseThrow().getName()).isEqualTo("Estoque");
        assertThat(repository.findById(sector.getId()).orElseThrow().getType()).isEqualTo("Operacional");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"Estoque\"}", "{\"name\":\"   \",\"type\":\"Operacional\"}"})
    void shouldRejectInvalidRegistration(String body) throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldReturnNotFoundForMissingSector() throws Exception {
        mvc.perform(get(BASE + "/find/{id}", -1L).with(user("tester")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Sector not found"));
        mvc.perform(get(BASE + "/find/name").param("name", "Inexistente").with(user("tester")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Sector not found"));
        mvc.perform(patch(BASE + "/update/{id}", -1L).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Novo\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Sector not found"));
        mvc.perform(delete(BASE + "/delete/{id}", -1L).with(user("tester")).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Sector not found"));
    }

    @Test
    void shouldNormalizeRegistrationAndSearchFields() throws Exception {
        mvc.perform(post(BASE + "/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" Estoque \",\"type\":\" Operacional \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Estoque"))
                .andExpect(jsonPath("$.type").value("Operacional"));
        Long id = repository.findByName("Estoque").orElseThrow().getId();
        mvc.perform(get(BASE + "/find/name").param("name", " Estoque ").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(BASE + "/find/type").param("type", " Operacional ").with(user("tester")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void shouldAcceptNameWithExactlyOneHundredCharacters() throws Exception {
        String name = "A".repeat(100);
        mvc.perform(post(BASE + "/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"type\":\"Operacional\"}".formatted(name)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(name));
        Long id = repository.findByName(name).orElseThrow().getId();
        String updatedName = "B".repeat(100);
        mvc.perform(patch(BASE + "/update/{id}", id).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted(updatedName)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(updatedName));
        repository.flush();
        assertThat(repository.findById(id).orElseThrow().getName()).isEqualTo(updatedName);
    }

    @Test
    void shouldRejectNameLongerThanOneHundredCharacters() throws Exception {
        String name = "A".repeat(101);
        mvc.perform(post(BASE + "/registration").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"type\":\"Operacional\"}".formatted(name)))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
        Sector sector = repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        mvc.perform(patch(BASE + "/update/{id}", sector.getId()).with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"type\":\"Outro\"}".formatted(name)))
                .andExpect(status().isBadRequest());
        Sector unchanged = repository.findById(sector.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Estoque");
        assertThat(unchanged.getType()).isEqualTo("Operacional");
    }

    @Test
    void shouldEnforceUniqueNameInDatabase() {
        repository.saveAndFlush(new Sector(null, "Estoque", "Operacional"));
        assertThatThrownBy(() -> repository.saveAndFlush(new Sector(null, "Estoque", "Outro")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowFrontendPatchPreflight() throws Exception {
        mvc.perform(options(BASE + "/update/1")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
