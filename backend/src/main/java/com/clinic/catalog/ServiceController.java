package com.clinic.catalog;

import com.clinic.catalog.dto.CreateServiceRequest;
import com.clinic.catalog.dto.ServiceResponse;
import com.clinic.catalog.dto.UpdateServiceRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service catalogue endpoints.
 *
 * Reads (GET) are open to any authenticated user — the booking flow needs them.
 * Edits (create/update/deactivate) are ADMIN-only (feature F8), enforced by
 * @PreAuthorize. Deactivating instead of deleting keeps past appointments intact,
 * and price snapshotting means edits never change already-booked bills.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final CatalogService catalogService;

    public ServiceController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Active services only — for the booking UI. */
    @GetMapping
    public List<ServiceResponse> listServices() {
        return catalogService.listActiveServices();
    }

    /** All services incl. inactive — the admin catalogue view. */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ServiceResponse> listAll() {
        return catalogService.listAllServices();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody CreateServiceRequest request) {
        return catalogService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse update(
            @PathVariable Long id, @Valid @RequestBody UpdateServiceRequest request) {
        return catalogService.update(id, request);
    }

    /** Soft-delete a service so it's no longer offered for booking. */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ServiceResponse deactivate(@PathVariable Long id) {
        return catalogService.setActive(id, false);
    }
}
