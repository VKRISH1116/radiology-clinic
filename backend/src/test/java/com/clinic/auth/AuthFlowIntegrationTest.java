package com.clinic.auth;

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
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end auth flow through the real HTTP + Spring Security stack (MockMvc) on
 * H2. Covers registration (patient-only), login, the refresh/logout cycle, and
 * admin-only user creation. The bootstrap ADMIN is provisioned at startup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AuthFlowIntegrationTest {

    // Matches application.yml app.bootstrap-admin.* dev defaults.
    private static final String ADMIN_EMAIL = "admin@clinic.local";
    private static final String ADMIN_PASSWORD = "admin-dev-password";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_createsPatient_returns201() throws Exception {
        register("reg1@clinic.test", "secret123").andExpect(status().isCreated());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register("dup@clinic.test", "secret123").andExpect(status().isCreated());
        register("dup@clinic.test", "secret123").andExpect(status().isConflict());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        register("short@clinic.test", "short").andExpect(status().isBadRequest());
    }

    @Test
    void publicRegistration_ignoresAnyRole_alwaysCreatesPatient() throws Exception {
        // Even if a caller smuggles "role":"ADMIN" in the JSON, the field is ignored
        // and the account is a PATIENT — the fix for the privilege-escalation hole.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sneaky@clinic.test\",\"password\":\"secret123\","
                                + "\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
        String token = accessToken("sneaky@clinic.test", "secret123");
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_PATIENT"));
    }

    @Test
    void login_validCredentials_returnsAccessAndRefreshTokens() throws Exception {
        register("login1@clinic.test", "secret123").andExpect(status().isCreated());
        login("login1@clinic.test", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        register("login2@clinic.test", "secret123").andExpect(status().isCreated());
        login("login2@clinic.test", "WRONGpass").andExpect(status().isUnauthorized());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsIdentity() throws Exception {
        register("me1@clinic.test", "secret123").andExpect(status().isCreated());
        String token = accessToken("me1@clinic.test", "secret123");
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me1@clinic.test"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_PATIENT"));
    }

    @Test
    void refresh_returnsNewTokens_andOldRefreshTokenIsRevoked() throws Exception {
        register("refresh1@clinic.test", "secret123").andExpect(status().isCreated());
        String firstRefresh =
                JsonPath.read(loginBody("refresh1@clinic.test", "secret123"), "$.refreshToken");

        String refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // The new access token works...
        String newAccess = JsonPath.read(refreshed, "$.token");
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk());

        // ...and the used refresh token is now rotated out (rejected on reuse).
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        register("logout1@clinic.test", "secret123").andExpect(status().isCreated());
        String refresh =
                JsonPath.read(loginBody("logout1@clinic.test", "secret123"), "$.refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateStaff_whoCanThenLogInWithStaffRole() throws Exception {
        String adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newstaff@clinic.test\",\"password\":\"secret123\","
                                + "\"role\":\"STAFF\"}"))
                .andExpect(status().isCreated());

        login("newstaff@clinic.test", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void createUser_byNonAdmin_isForbidden() throws Exception {
        register("plainpatient@clinic.test", "secret123").andExpect(status().isCreated());
        String patientToken = accessToken("plainpatient@clinic.test", "secret123");
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@clinic.test\",\"password\":\"secret123\","
                                + "\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"y@clinic.test\",\"password\":\"secret123\","
                                + "\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---------------------------------------------------------

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String loginBody(String email, String password) throws Exception {
        return login(email, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String accessToken(String email, String password) throws Exception {
        return JsonPath.read(loginBody(email, password), "$.token");
    }
}
