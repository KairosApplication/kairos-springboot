package com.kairos.kairosapipostgres.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test
    void shouldPublishDocumentedRoutesAndSessionAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Kairos API"))
                .andExpect(jsonPath("$.components.securitySchemes.sessionCookie.name").value("JSESSIONID"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.requestBody.content"
                        + "['application/x-www-form-urlencoded']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/registration'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customers/{id}/recommendations'].get.summary")
                        .value("Recomendar produtos"))
                .andExpect(jsonPath("$.paths['/api/v1/purchases/{id}/total'].get.security[0]"
                        + ".sessionCookie").exists());
    }
}
