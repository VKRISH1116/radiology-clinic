package com.clinic.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The health endpoint must be reachable with NO auth (an external uptime monitor
 * has no token) and must report the database as UP when it's reachable — which it
 * always is under @SpringBootTest on the H2 profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class HealthCheckIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_isPublic_andReportsUp() throws Exception {
        mockMvc.perform(get("/health")) // deliberately no Authorization header
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.db").value("UP"))
                .andExpect(jsonPath("$.time").exists());
    }
}
