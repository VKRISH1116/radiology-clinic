package com.clinic.catalog.dto;

import com.clinic.catalog.Service;
import java.math.BigDecimal;

/**
 * What the API returns for one catalogue entry. A DTO (not the entity) is sent
 * over the wire so the JSON shape is decoupled from the DB mapping. The active
 * flag is always true on the public list (it only contains active services), but
 * meaningful on the admin /all view.
 */
public record ServiceResponse(Long id, String category, String name, BigDecimal price, boolean active) {

    /** Map a persisted entity to its response shape. */
    public static ServiceResponse from(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getCategory(),
                service.getName(),
                service.getPrice(),
                service.isActive());
    }
}
