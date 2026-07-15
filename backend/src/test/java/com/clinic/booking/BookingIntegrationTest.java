package com.clinic.booking;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clinic.consent.ConsentRecordRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end booking flow on H2. Each test uses its own future date and its own
 * user, so tests stay isolated on the shared in-memory database regardless of
 * order (slots are keyed by a unique start_time, so distinct dates never clash).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class BookingIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@clinic.local";
    private static final String ADMIN_PASSWORD = "admin-dev-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConsentRecordRepository consentRecordRepository;

    @Test
    void slots_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/slots").param("date", "2030-01-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void slots_withToken_returns48SlotsForTheDay() throws Exception {
        String token = registerAndLogin("slots.reader@clinic.test");
        mockMvc.perform(get("/api/slots").param("date", "2030-02-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(48))
                .andExpect(jsonPath("$[0].available").value(1));
    }

    @Test
    void book_validRequest_snapshotsPricesAndBillsTheSum() throws Exception {
        String token = registerAndLogin("booker1@clinic.test");
        long slotId = firstSlotId(token, "2030-03-01");

        // Services 1 (Abdomen & Pelvis, 1000) + 4 (Thyroid, 1500) = 2500.
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + slotId + ",\"serviceIds\":[1,4],"
                                + "\"patient\":{\"fullName\":\"Asha Rao\",\"gender\":\"FEMALE\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.billedAmount").value(2500.00))
                .andExpect(jsonPath("$.studies.length()").value(2))
                .andExpect(jsonPath("$.studies[0].priceSnapshot").value(1000.00))
                .andExpect(jsonPath("$.studies[1].priceSnapshot").value(1500.00));
    }

    @Test
    void book_sameSlotBeyondCapacity_returns409() throws Exception {
        String token = registerAndLogin("booker2@clinic.test");
        long slotId = firstSlotId(token, "2030-04-01");

        book(token, slotId, "[1]").andExpect(status().isCreated());
        // Default capacity is 1, so the second booking of the same slot is refused.
        book(token, slotId, "[1]").andExpect(status().isConflict());
    }

    @Test
    void book_unknownService_returns400() throws Exception {
        String token = registerAndLogin("booker3@clinic.test");
        long slotId = firstSlotId(token, "2030-05-01");
        book(token, slotId, "[999]").andExpect(status().isBadRequest());
    }

    @Test
    void book_unknownSlot_returns404() throws Exception {
        String token = registerAndLogin("booker4@clinic.test");
        book(token, 999999L, "[1]").andExpect(status().isNotFound());
    }

    @Test
    void book_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":1,\"serviceIds\":[1],"
                                + "\"patient\":{\"fullName\":\"X\"}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myAppointments_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/appointments/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myAppointments_neverBooked_returnsEmptyList() throws Exception {
        String token = registerAndLogin("emptymine@clinic.test");
        mockMvc.perform(get("/api/appointments/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void myAppointments_afterBooking_listsIt() throws Exception {
        String token = registerAndLogin("listmine@clinic.test");
        long slotId = firstSlotId(token, "2030-06-01");
        book(token, slotId, "[1]").andExpect(status().isCreated());

        mockMvc.perform(get("/api/appointments/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("BOOKED"));
    }

    @Test
    void cancel_ownBookedAppointment_setsCancelledAndFreesSlot() throws Exception {
        String token = registerAndLogin("canceller@clinic.test");
        String date = "2030-07-01";
        long slotId = firstSlotId(token, date);
        long appointmentId = bookReturningId(token, slotId, "[1]");

        // Slot is taken right after booking...
        mockMvc.perform(get("/api/slots").param("date", date)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].available").value(0));

        cancel(token, appointmentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // ...and free again once cancelled.
        mockMvc.perform(get("/api/slots").param("date", date)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].available").value(1));
    }

    @Test
    void cancel_alreadyCancelled_returns409() throws Exception {
        String token = registerAndLogin("canceltwice@clinic.test");
        long slotId = firstSlotId(token, "2030-08-01");
        long appointmentId = bookReturningId(token, slotId, "[1]");

        cancel(token, appointmentId).andExpect(status().isOk());
        cancel(token, appointmentId).andExpect(status().isConflict());
    }

    @Test
    void cancel_someoneElsesAppointment_returns404() throws Exception {
        String owner = registerAndLogin("owner@clinic.test");
        long slotId = firstSlotId(owner, "2030-09-01");
        long appointmentId = bookReturningId(owner, slotId, "[1]");

        String intruder = registerAndLogin("intruder@clinic.test");
        cancel(intruder, appointmentId).andExpect(status().isNotFound());
    }

    @Test
    void cancel_unknownId_returns404() throws Exception {
        String token = registerAndLogin("cancelunknown@clinic.test");
        cancel(token, 999999L).andExpect(status().isNotFound());
    }

    @Test
    void cancel_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/appointments/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reschedule_movesToAnotherSlot_freeingTheOldOne() throws Exception {
        String token = registerAndLogin("resched@clinic.test");
        String dayA = "2032-01-01";
        String dayB = "2032-01-02";
        long slotA = firstSlotId(token, dayA);
        long appointmentId = bookReturningId(token, slotA, "[1]");
        long slotB = firstSlotId(token, dayB);

        reschedule(token, appointmentId, slotB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value((int) slotB));

        // Old slot free again, new slot taken.
        mockMvc.perform(get("/api/slots").param("date", dayA)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].available").value(1));
        mockMvc.perform(get("/api/slots").param("date", dayB)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].available").value(0));
    }

    @Test
    void reschedule_toAFullSlot_returns409() throws Exception {
        String other = registerAndLogin("resched.filler@clinic.test");
        long fullSlot = firstSlotId(other, "2032-02-02");
        book(other, fullSlot, "[1]").andExpect(status().isCreated()); // capacity 1 now used

        String token = registerAndLogin("resched.mover@clinic.test");
        long slotA = firstSlotId(token, "2032-02-01");
        long appointmentId = bookReturningId(token, slotA, "[1]");
        reschedule(token, appointmentId, fullSlot).andExpect(status().isConflict());
    }

    @Test
    void reschedule_someoneElsesAppointment_returns404() throws Exception {
        String owner = registerAndLogin("resched.owner@clinic.test");
        long slotA = firstSlotId(owner, "2032-03-01");
        long appointmentId = bookReturningId(owner, slotA, "[1]");
        long slotB = firstSlotId(owner, "2032-03-02");

        String intruder = registerAndLogin("resched.intruder@clinic.test");
        reschedule(intruder, appointmentId, slotB).andExpect(status().isNotFound());
    }

    @Test
    void reschedule_aCancelledAppointment_returns409() throws Exception {
        String token = registerAndLogin("resched.cancelled@clinic.test");
        long slotA = firstSlotId(token, "2032-04-01");
        long appointmentId = bookReturningId(token, slotA, "[1]");
        cancel(token, appointmentId).andExpect(status().isOk());

        long slotB = firstSlotId(token, "2032-04-02");
        reschedule(token, appointmentId, slotB).andExpect(status().isConflict());
    }

    @Test
    void selfBooking_recordsConsentForTheNewPatient() throws Exception {
        String token = registerAndLogin("consent.pat@clinic.test");
        long slotId = firstSlotId(token, "2034-01-01");
        String body = book(token, slotId, "[1]")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long patientId = ((Number) JsonPath.read(body, "$.patientId")).longValue();
        assertTrue(consentRecordRepository.existsByPatientId(patientId));
    }

    @Test
    void walkInBooking_byStaffOrAdmin_createsAnUnlinkedPatientWithConsent() throws Exception {
        String admin = loginToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long slotId = firstSlotId(admin, "2034-02-01");
        String body = mockMvc.perform(post("/api/appointments/walk-in")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + slotId + ",\"serviceIds\":[1],"
                                + "\"patient\":{\"fullName\":\"Walk In\"}}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long patientId = ((Number) JsonPath.read(body, "$.patientId")).longValue();
        assertTrue(consentRecordRepository.existsByPatientId(patientId));
    }

    @Test
    void walkInBooking_byPatient_isForbidden() throws Exception {
        String patient = registerAndLogin("walkin.pat@clinic.test");
        long slotId = firstSlotId(patient, "2034-03-01");
        mockMvc.perform(post("/api/appointments/walk-in")
                        .header("Authorization", "Bearer " + patient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + slotId + ",\"serviceIds\":[1],"
                                + "\"patient\":{\"fullName\":\"Nope\"}}"))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---------------------------------------------------------

    private String loginToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private org.springframework.test.web.servlet.ResultActions reschedule(
            String token, long appointmentId, long slotId) throws Exception {
        return mockMvc.perform(post("/api/appointments/" + appointmentId + "/reschedule")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":" + slotId + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions book(
            String token, long slotId, String serviceIdsJson) throws Exception {
        return mockMvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":" + slotId + ",\"serviceIds\":" + serviceIdsJson + ","
                        + "\"patient\":{\"fullName\":\"Test Patient\"}}"));
    }

    private long bookReturningId(String token, long slotId, String serviceIdsJson) throws Exception {
        String body = book(token, slotId, serviceIdsJson)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions cancel(String token, long id)
            throws Exception {
        return mockMvc.perform(post("/api/appointments/" + id + "/cancel")
                .header("Authorization", "Bearer " + token));
    }

    /** Generate the day (via GET /api/slots) and return its first slot's id. */
    private long firstSlotId(String token, String date) throws Exception {
        String body = mockMvc.perform(get("/api/slots").param("date", date)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    private String registerAndLogin(String email) throws Exception {
        String json = "{\"email\":\"" + email + "\",\"password\":\"secret123\"}";
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
