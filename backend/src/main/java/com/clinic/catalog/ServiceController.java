package com.clinic.catalog;

import com.clinic.catalog.dto.ServiceResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoint for the service catalogue, consumed by the booking flow.
 *
 * Security: this is not under /api/auth/**, so SecurityConfig's
 * anyRequest().authenticated() applies — any logged-in user (PATIENT, STAFF or
 * ADMIN) may list services. Editing the catalogue will be a separate, ADMIN-only
 * slice later; that is where role-based restriction belongs.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final CatalogService catalogService;

    public ServiceController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ServiceResponse> listServices() {
        return catalogService.listActiveServices();
    }
}
