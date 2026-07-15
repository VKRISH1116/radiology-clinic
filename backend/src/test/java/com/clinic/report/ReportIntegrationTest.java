package com.clinic.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end report upload/download on H2, including access control. Uploads come
 * from the bootstrap ADMIN (which satisfies STAFF/ADMIN); a patient books first so
 * there's an appointment to attach a report to.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ReportIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@clinic.local";
    private static final String ADMIN_PASSWORD = "admin-dev-password";
    private static final byte[] PDF_BYTES = "%PDF-1.4\ntest report\n%%EOF".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void staffUploads_thenOwningPatientCanDownload() throws Exception {
        String patient = registerAndLogin("rep.owner@clinic.test");
        long appointmentId = book(patient, "2035-01-01");
        String admin = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        upload(admin, appointmentId, pdf()).andExpect(status().isCreated());

        byte[] downloaded = mockMvc.perform(get("/api/appointments/" + appointmentId + "/report")
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertArrayEquals(PDF_BYTES, downloaded);
    }

    @Test
    void anotherPatient_cannotDownloadSomeoneElsesReport() throws Exception {
        String owner = registerAndLogin("rep.owner2@clinic.test");
        long appointmentId = book(owner, "2035-02-01");
        String admin = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        upload(admin, appointmentId, pdf()).andExpect(status().isCreated());

        String other = registerAndLogin("rep.intruder@clinic.test");
        mockMvc.perform(get("/api/appointments/" + appointmentId + "/report")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_byPatient_isForbidden() throws Exception {
        String patient = registerAndLogin("rep.patupload@clinic.test");
        long appointmentId = book(patient, "2035-03-01");
        upload(patient, appointmentId, pdf()).andExpect(status().isForbidden());
    }

    @Test
    void upload_nonPdf_isRejected() throws Exception {
        String patient = registerAndLogin("rep.nonpdf@clinic.test");
        long appointmentId = book(patient, "2035-04-01");
        String admin = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        MockMultipartFile txt =
                new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        upload(admin, appointmentId, txt).andExpect(status().isBadRequest());
    }

    @Test
    void download_whenNoReportUploaded_returns404() throws Exception {
        String patient = registerAndLogin("rep.noreport@clinic.test");
        long appointmentId = book(patient, "2035-05-01");
        mockMvc.perform(get("/api/appointments/" + appointmentId + "/report")
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---------------------------------------------------------

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "report.pdf", "application/pdf", PDF_BYTES);
    }

    private org.springframework.test.web.servlet.ResultActions upload(
            String token, long appointmentId, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/appointments/" + appointmentId + "/report")
                .file(file)
                .header("Authorization", "Bearer " + token));
    }

    private long book(String token, String date) throws Exception {
        String slotBody = mockMvc.perform(get("/api/slots").param("date", date)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long slotId = ((Number) JsonPath.read(slotBody, "$[0].id")).longValue();

        String body = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + slotId + ",\"serviceIds\":[1],"
                                + "\"patient\":{\"fullName\":\"Rep Patient\"}}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String registerAndLogin(String email) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        return loginToken(email, "secret123");
    }

    private String loginToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
