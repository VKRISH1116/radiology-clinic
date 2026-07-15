package com.clinic.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data derives the SQL from these method names — no implementation needed.
 * findByEmail -> "SELECT ... WHERE email = ?"; existsByEmail -> "SELECT count(...) ...".
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
