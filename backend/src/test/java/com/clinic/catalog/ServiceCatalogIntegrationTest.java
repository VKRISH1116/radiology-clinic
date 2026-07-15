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
        String token = registerAndLogin("catalog.reader@clinic.test");

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

    @Test
    void adminCreatesService_thenItIsBookable() throws Exception {
        String admin = adminToken();
        String created = mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"General\",\"name\":\"Ultrasound Knee\","
                                + "\"price\":1200}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ultrasound Knee"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        // It now shows up in the active catalogue any user can read.
        String patient = registerAndLogin("cat.reader2@clinic.test");
        mockMvc.perform(get("/api/services").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].name").value("Ultrasound Knee"));
    }

    @Test
    void createService_byNonAdmin_isForbidden() throws Exception {
        String patient = registerAndLogin("cat.nonadmin@clinic.test");
        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"General\",\"name\":\"X\",\"price\":100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatedService_dropsOutOfTheActiveCatalogue() throws Exception {
        String admin = adminToken();
        String created = mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"General\",\"name\":\"Ultrasound Elbow\","
                                + "\"price\":900}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(post("/api/services/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        String patient = registerAndLogin("cat.reader3@clinic.test");
        mockMvc.perform(get("/api/services").header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    // --- helpers ---------------------------------------------------------

    /** Any authenticated user may read the catalogue; register a patient to get a token. */
    private String registerAndLogin(String email) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        return loginToken(json);
    }

    private String adminToken() throws Exception {
        // The bootstrap ADMIN (application.yml dev defaults).
        return loginToken("{\"email\":\"admin@clinic.local\",\"password\":\"admin-dev-password\"}");
    }

    private String loginToken(String credentialsJson) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(credentialsJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
