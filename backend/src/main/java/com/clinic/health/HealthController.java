package com.clinic.health;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public health check for uptime monitoring.
 *
 * A monitor (UptimeRobot, Better Stack, Render, ...) pings this URL every few
 * minutes; a non-200 response is how we learn about downtime BEFORE a user does.
 * It's intentionally the only unauthenticated read endpoint (permitted in
 * SecurityConfig), and it takes no token so an external monitor can reach it.
 *
 * This is a READINESS check, not just liveness: it also verifies the database is
 * reachable, so a DB outage surfaces as 503 (DOWN) rather than a silent 200.
 * The DB ping is deliberately cheap (Connection.isValid) — no query, no table.
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = isDatabaseReachable();
        HttpStatus status = dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(Map.of(
                "status", dbUp ? "UP" : "DOWN",
                "db", dbUp ? "UP" : "DOWN",
                "time", Instant.now().toString()));
    }

    /** Cheap connectivity probe: borrow a pooled connection and validate it. */
    private boolean isDatabaseReachable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2); // seconds to wait for validation
        } catch (Exception e) {
            return false;
        }
    }
}
