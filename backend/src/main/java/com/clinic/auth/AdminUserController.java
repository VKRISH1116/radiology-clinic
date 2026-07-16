package com.clinic.auth;

import com.clinic.auth.dto.CreateUserRequest;
import com.clinic.auth.dto.UserSummaryResponse;
import com.clinic.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only user management — the controlled path to create STAFF/ADMIN accounts
 * (public registration can only make patients). ADMIN is enforced by method
 * security; the very first admin is provisioned at startup by {@code BootstrapAdmin}.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AdminUserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummaryResponse> list() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummaryResponse(u.getEmail(), u.getRole().name()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@Valid @RequestBody CreateUserRequest request) {
        authService.createUser(request);
    }
}
