package com.clinic.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end test for the service-catalogue read endpoint, through the real HTTP
 * + Spring Security stack (MockMvc) on the in-memory H2 database. The 9 services
 * come from Flyway V2 seed data, which runs on H2 exactly as on Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ServiceCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listServices_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listServices_withToken_returnsSeededCatalogue() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/services").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // V2 seeds exactly 9 active services.
                .andExpect(jsonPath("$.length()").value(9))
                // Sorted category-asc, price-asc, name-asc: General/1000 comes first.
                .andExpect(jsonPath("$[0].category").value("General"))
                .andExpect(jsonPath("$[0].name").value("Ultrasound Abdomen & Pelvis"))
                .andExpect(jsonPath("$[0].price").value(1000.00))
                // The internal "active" flag is not exposed in the response.
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    /** Any authenticated user may read the catalogue; register a patient to get a token. */
    private String registerAndLogin() throws Exception {
        String json = "{\"email\":\"catalog.reader@clinic.test\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
