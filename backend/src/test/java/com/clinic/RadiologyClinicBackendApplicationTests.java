package com.clinic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2") // use the in-memory DB so the context loads without Postgres running
class RadiologyClinicBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
