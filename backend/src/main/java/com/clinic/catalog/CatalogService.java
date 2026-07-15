package com.clinic.catalog;

import com.clinic.catalog.dto.ServiceResponse;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-service layer for the catalogue (Controller -> Service -> Repository,
 * per the Architecture ADR). For a read this layer is thin, but it keeps the
 * controller free of persistence concerns and gives write operations (admin
 * catalogue editing, later) a home.
 *
 * Note the fully-qualified {@code @org.springframework.stereotype.Service} below:
 * our JPA entity is also named {@link Service}, so importing Spring's @Service
 * annotation would make the simple name "Service" ambiguous in this file.
 * Fully-qualifying the annotation lets "Service"/"ServiceResponse" refer to our
 * own types without an alias.
 */
@org.springframework.stereotype.Service
public class CatalogService {

    private final ServiceRepository serviceRepository;

    public CatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /** All bookable scans, grouped by category and cheapest-first. */
    @Transactional(readOnly = true)
    public List<ServiceResponse> listActiveServices() {
        return serviceRepository.findByActiveTrueOrderByCategoryAscPriceAscNameAsc()
                .stream()
                .map(ServiceResponse::from)
                .toList();
    }
}
