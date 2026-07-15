package com.clinic.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for the service catalogue. Spring Data derives the query from the
 * method name: findByActiveTrue... -> "WHERE active = true", and the OrderBy
 * suffix adds the sort. So the booking UI receives only bookable scans, grouped
 * by category and cheapest-first — no hand-written SQL needed.
 */
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByActiveTrueOrderByCategoryAscPriceAscNameAsc();
}
