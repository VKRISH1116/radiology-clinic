package com.clinic.catalog;

import com.clinic.catalog.dto.CreateServiceRequest;
import com.clinic.catalog.dto.ServiceResponse;
import com.clinic.catalog.dto.UpdateServiceRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    /** Every service including inactive ones — the admin catalogue view (F8). */
    @Transactional(readOnly = true)
    public List<ServiceResponse> listAllServices() {
        return serviceRepository.findAll().stream().map(ServiceResponse::from).toList();
    }

    @Transactional
    public ServiceResponse create(CreateServiceRequest request) {
        Service service = new Service();
        service.setCategory(request.category());
        service.setName(request.name());
        service.setPrice(request.price());
        service.setActive(request.active() == null ? true : request.active());
        return ServiceResponse.from(serviceRepository.save(service));
    }

    @Transactional
    public ServiceResponse update(Long id, UpdateServiceRequest request) {
        Service service = requireService(id);
        service.setCategory(request.category());
        service.setName(request.name());
        service.setPrice(request.price());
        service.setActive(request.active());
        return ServiceResponse.from(serviceRepository.save(service));
    }

    /** Soft-delete: deactivate rather than remove, so past appointments stay intact. */
    @Transactional
    public ServiceResponse setActive(Long id, boolean active) {
        Service service = requireService(id);
        service.setActive(active);
        return ServiceResponse.from(serviceRepository.save(service));
    }

    private Service requireService(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }
}
