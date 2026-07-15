package com.clinic.referral;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end referral flow on H2. Doctors and rules come from the V3 seed (which
 * runs on H2 too): doctor 1 = Dr. Meera Sharma with a 25% doctor-specific rule.
 * Each test uses its own future date and users so it stays isolated on shared H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ReferralIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void booking_withUnknownDoctor_returns400() throws Exception {
        String patient = registerAndLogin("ref.baddoc@clinic.test", null);
        long slotId = firstSlotId(patient, "2031-01-01");
        book(patient, slotId, "[1]", 999999L).andExpect(status().isBadRequest());
    }

    @Test
    void complete_byPatient_isForbidden() throws Exception {
        String patient = registerAndLogin("ref.pat.complete@clinic.test", null);
        long slotId = firstSlotId(patient, "2031-02-01");
        long appointmentId = bookReturningId(patient, slotId, "[1]", 1L);
        mockMvc.perform(post("/api/appointments/" + appointmentId + "/complete")
                        .header("Authorization", "Bearer " + patient))
                .andExpect(status().isForbidden());
    }

    @Test
    void complete_byStaff_computesReferralByMostSpecificRule() throws Exception {
        String patient = registerAndLogin("ref.pat@clinic.test", null);
        String staff = registerAndLogin("ref.staff@clinic.test", "STAFF");

        long slotId = firstSlotId(patient, "2031-03-01");
        // Dr. Sharma (id 1, 25%) + Abdomen(1000) + Thyroid(1500) = 2500 -> 625.
        long appointmentId = bookReturningId(patient, slotId, "[1,4]", 1L);

        mockMvc.perform(post("/api/appointments/" + appointmentId + "/complete")
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk());

        String ledger = mockMvc.perform(get("/api/referrals")
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> amounts =
                JsonPath.read(ledger, "$[?(@.appointmentId == " + appointmentId + ")].amount");
        assertEquals(1, amounts.size());
        assertEquals(625.0, ((Number) amounts.get(0)).doubleValue());
    }

    @Test
    void completing_anAlreadyCompletedAppointment_returns409() throws Exception {
        String patient = registerAndLogin("ref.twice.pat@clinic.test", null);
        String staff = registerAndLogin("ref.twice.staff@clinic.test", "STAFF");
        long slotId = firstSlotId(patient, "2031-04-01");
        long appointmentId = bookReturningId(patient, slotId, "[1]", 1L);

        complete(staff, appointmentId).andExpect(status().isOk());
        complete(staff, appointmentId).andExpect(status().isConflict());
    }

    @Test
    void referralsLedger_patientForbidden_staffAllowed() throws Exception {
        String patient = registerAndLogin("ref.ledger.pat@clinic.test", null);
        String staff = registerAndLogin("ref.ledger.staff@clinic.test", "STAFF");
        mockMvc.perform(get("/api/referrals").header("Authorization", "Bearer " + patient))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/referrals").header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk());
    }

    @Test
    void referralRules_staffForbidden_adminAllowed() throws Exception {
        String staff = registerAndLogin("ref.rules.staff@clinic.test", "STAFF");
        String admin = registerAndLogin("ref.rules.admin@clinic.test", "ADMIN");
        mockMvc.perform(get("/api/referral-rules").header("Authorization", "Bearer " + staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/referral-rules").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void referral_markedPaid_byAdmin_andRepayReturns409() throws Exception {
        String patient = registerAndLogin("pay.pat@clinic.test", null);
        String staff = registerAndLogin("pay.staff@clinic.test", "STAFF");
        String admin = registerAndLogin("pay.admin@clinic.test", "ADMIN");

        long slotId = firstSlotId(patient, "2033-01-01");
        long appointmentId = bookReturningId(patient, slotId, "[1]", 1L);
        complete(staff, appointmentId).andExpect(status().isOk());
        long referralId = referralIdFor(staff, appointmentId);

        mockMvc.perform(post("/api/referrals/" + referralId + "/pay")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(post("/api/referrals/" + referralId + "/pay")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    void referral_pay_byNonAdmin_isForbidden() throws Exception {
        String staff = registerAndLogin("pay.nonadmin@clinic.test", "STAFF");
        mockMvc.perform(post("/api/referrals/1/pay")
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isForbidden());
    }

    @Test
    void payingAReferral_writesAPayoutAuditEntry() throws Exception {
        String patient = registerAndLogin("audit.pat@clinic.test", null);
        String staff = registerAndLogin("audit.staff@clinic.test", "STAFF");
        String admin = registerAndLogin("audit.admin@clinic.test", "ADMIN");

        long slotId = firstSlotId(patient, "2033-06-01");
        long appointmentId = bookReturningId(patient, slotId, "[1]", 1L);
        complete(staff, appointmentId).andExpect(status().isOk());
        long referralId = referralIdFor(staff, appointmentId);
        mockMvc.perform(post("/api/referrals/" + referralId + "/pay")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        String logs = mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> hits = JsonPath.read(logs,
                "$[?(@.action == 'PAYOUT_UPDATE' && @.entityId == " + referralId + ")]");
        org.junit.jupiter.api.Assertions.assertFalse(hits.isEmpty(), "expected a PAYOUT_UPDATE audit row");
    }

    @Test
    void auditLog_isForbiddenForNonAdmins() throws Exception {
        String staff = registerAndLogin("audit.staffonly@clinic.test", "STAFF");
        mockMvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + staff))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---------------------------------------------------------

    private long referralIdFor(String token, long appointmentId) throws Exception {
        String body = mockMvc.perform(get("/api/referrals")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> ids = JsonPath.read(body, "$[?(@.appointmentId == " + appointmentId + ")].id");
        return ((Number) ids.get(0)).longValue();
    }

    private ResultActions book(String token, long slotId, String serviceIdsJson, Long doctorId)
            throws Exception {
        String doctor = doctorId == null ? "" : ",\"referringDoctorId\":" + doctorId;
        return mockMvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":" + slotId + ",\"serviceIds\":" + serviceIdsJson + doctor
                        + ",\"patient\":{\"fullName\":\"Ref Patient\"}}"));
    }

    private long bookReturningId(String token, long slotId, String serviceIdsJson, Long doctorId)
            throws Exception {
        String body = book(token, slotId, serviceIdsJson, doctorId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions complete(String token, long id) throws Exception {
        return mockMvc.perform(post("/api/appointments/" + id + "/complete")
                .header("Authorization", "Bearer " + token));
    }

    private long firstSlotId(String token, String date) throws Exception {
        String body = mockMvc.perform(get("/api/slots").param("date", date)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    // Matches application.yml app.bootstrap-admin.* dev defaults.
    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@clinic.local";
    private static final String BOOTSTRAP_ADMIN_PASSWORD = "admin-dev-password";

    /**
     * Create an account and return its access token. Patients (role == null) use
     * public registration; STAFF/ADMIN are created via the admin endpoint, since
     * public registration can no longer grant elevated roles.
     */
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
