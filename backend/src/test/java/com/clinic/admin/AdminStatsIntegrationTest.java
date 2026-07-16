package com.clinic.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The admin dashboard KPI endpoint (AC-F7-1) on H2. Counts are global on the
 * shared in-memory DB, so assertions are "at least" checks after we create the
 * data this test cares about, rather than exact totals.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AdminStatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void stats_areAdminOnly() throws Exception {
        String patient = registerAndLogin("stats.pat@clinic.test", null);
        String staff = registerAndLogin("stats.staff@clinic.test", "STAFF");
        String admin = registerAndLogin("stats.admin@clinic.test", "ADMIN");

        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + patient))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void stats_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void stats_countTodaysAppointmentsAndPendingReports() throws Exception {
        String patient = registerAndLogin("stats.count.pat@clinic.test", null);
        String staff = registerAndLogin("stats.count.staff@clinic.test", "STAFF");
        String admin = registerAndLogin("stats.count.admin@clinic.test", "ADMIN");

        // Book a visit for TODAY (the KPI is scoped to the clinic's local day).
        String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
        long slotId = firstSlotId(patient, today);
        long appointmentId = bookReturningId(patient, slotId, "[1]");

        // Complete it but upload no report -> it is a "pending report".
        mockMvc.perform(post("/api/appointments/" + appointmentId + "/complete")
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topReferrers").isArray())
                .andReturn().getResponse().getContentAsString();

        long todays = ((Number) JsonPath.read(body, "$.todaysAppointments")).longValue();
        long totalPatients = ((Number) JsonPath.read(body, "$.totalPatients")).longValue();
        long pending = ((Number) JsonPath.read(body, "$.reportsPending")).longValue();

        assertTrue(todays >= 1, "today's appointment should be counted");
        assertTrue(totalPatients >= 1, "the booking patient should be counted");
        assertTrue(pending >= 1, "the completed-without-report visit should be pending");
    }

    // --- helpers (mirrors the other integration tests) --------------------

    private ResultActions book(String token, long slotId, String serviceIdsJson) throws Exception {
        return mockMvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":" + slotId + ",\"serviceIds\":" + serviceIdsJson
                        + ",\"patient\":{\"fullName\":\"Stats Patient\"}}"));
    }

    private long bookReturningId(String token, long slotId, String serviceIdsJson) throws Exception {
        String body = book(token, slotId, serviceIdsJson)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long firstSlotId(String token, String date) throws Exception {
        String body = mockMvc.perform(get("/api/slots").param("date", date)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

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
