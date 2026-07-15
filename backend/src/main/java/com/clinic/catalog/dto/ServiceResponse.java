package com.clinic.catalog.dto;

import com.clinic.catalog.Service;
import java.math.BigDecimal;

/**
 * What the API returns for one catalogue entry. A DTO (not the entity) is sent
 * over the wire so the JSON shape is decoupled from the DB mapping: we expose
 * only what a client needs (id/category/name/price) and omit internals like the
 * "active" flag — the list already contains active services only.
 */
public record ServiceResponse(Long id, String category, String name, BigDecimal price) {

    /** Map a persisted entity to its response shape. */
    public static ServiceResponse from(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getCategory(),
                service.getName(),
                service.getPrice());
    }
}
