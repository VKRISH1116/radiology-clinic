package com.clinic.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clinic.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end auth flow through the real HTTP + Spring Security stack (MockMvc),
 * backed by the in-memory H2 database (Flyway builds the schema). This is the
 * automated version of the manual curl checks we ran while building the slice.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AuthFlowIntegrationTest {

    private static final String ADMIN_JSON =
            "{\"email\":\"dr.rao@clinic.test\",\"password\":\"secret123\",\"role\":\"ADMIN\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void resetUsers() {
        // H2 is in-memory but shared across a test class; start each test clean.
        userRepository.deleteAll();
    }

    @Test
    void register_createsUser_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register(ADMIN_JSON);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(ADMIN_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@y.test\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        register(ADMIN_JSON);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dr.rao@clinic.test\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("dr.rao@clinic.test"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        register(ADMIN_JSON);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dr.rao@clinic.test\",\"password\":\"WRONG\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@clinic.test\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsIdentity() throws Exception {
        register(ADMIN_JSON);
        String token = login("dr.rao@clinic.test", "secret123");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dr.rao@clinic.test"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void register_withoutRole_defaultsToPatient() throws Exception {
        register("{\"email\":\"patient1@clinic.test\",\"password\":\"secret123\"}");
        String token = login("patient1@clinic.test", "secret123");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_PATIENT"));
    }

    // --- helpers ---------------------------------------------------------

    private void register(String json) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
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
