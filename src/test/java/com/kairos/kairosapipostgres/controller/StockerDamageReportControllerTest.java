package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.service.StockerDamageReportService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StockerDamageReportControllerTest {
    @Test
    void routesPassAuthenticatedIdentityAndDisableCaching() throws Exception {
        var service = mock(StockerDamageReportService.class);
        var actor = new UsernamePasswordAuthenticationToken("stocker@invalid", null, List.of());
        when(service.list(actor)).thenReturn(List.of(Map.of("id", 91L)));
        when(service.resolve(actor, 91L)).thenReturn(Map.of("alertId", 91L, "status", "RESOLVED"));
        var mvc = MockMvcBuilders.standaloneSetup(new StockerDamageReportController(service)).build();
        mvc.perform(get("/api/v1/alerts/customer-reports").principal(actor))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].id").value(91));
        mvc.perform(patch("/api/v1/alerts/customer-reports/91/resolve").principal(actor))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.alertId").value(91)).andExpect(jsonPath("$.status").value("RESOLVED"));
        verify(service).list(actor);
        verify(service).resolve(actor, 91L);
    }
}
