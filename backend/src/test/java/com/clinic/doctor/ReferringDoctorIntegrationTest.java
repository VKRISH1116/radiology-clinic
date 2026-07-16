package com.clinic.doctor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Adding a referring doctor on the fly (the walk-in "pick-or-type" field) on H2.
 * Creating is STAFF/ADMIN; listing is open to any authenticated user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ReferringDoctorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void staffCanCreateADoctor_andItShowsInTheList() throws Exception {
        String staff = registerAndLogin("doc.staff@clinic.test", "STAFF");

        mockMvc.perform(post("/api/referring-doctors")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dr. Neha Verma\",\"phone\":\"9811111111\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Dr. Neha Verma"));

        String list = mockMvc.perform(get("/api/referring-doctors")
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> hits = JsonPath.read(list, "$[?(@.name == 'Dr. Neha Verma')]");
        assertTrue(hits.size() >= 1, "the new doctor should appear in the active list");
    }

    @Test
    void patientCannotCreateADoctor() throws Exception {
        String patient = registerAndLogin("doc.pat@clinic.test", null);
        mockMvc.perform(post("/api/referring-doctors")
                        .header("Authorization", "Bearer " + patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dr. Sneaky\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void blankName_returns400() throws Exception {
        String staff = registerAndLogin("doc.blank.staff@clinic.test", "STAFF");
        mockMvc.perform(post("/api/referring-doctors")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---------------------------------------------------------

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@clinic.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "admin-dev-password";

    private String registerAndLogin(String email, String role) throws Exception {
        if (role == null) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                    .andExpect(status().isCreated());
        } else {
            String adminToken = login(BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD);
            mockMvc.perform(post("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"secret123\","
                                    + "\"role\":\"" + role + "\"}"))
                    .andExpect(status().isCreated());
        }
        return login(email, "secret123");
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
