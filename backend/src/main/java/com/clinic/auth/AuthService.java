package com.clinic.auth;

import com.clinic.auth.dto.AuthResponse;
import com.clinic.auth.dto.CreateUserRequest;
import com.clinic.auth.dto.LoginRequest;
import com.clinic.auth.dto.RegisterRequest;
import com.clinic.user.Role;
import com.clinic.user.User;
import com.clinic.user.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Registration, login, token refresh and logout. Passwords are only ever stored BCrypt-hashed. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /** Public self-registration. Always creates a PATIENT — never an elevated role. */
    @Transactional
    public void register(RegisterRequest request) {
        createAccount(request.email(), request.password(), Role.PATIENT);
    }

    /** Admin-only account creation (the controlled path to STAFF/ADMIN). */
    @Transactional
    public void createUser(CreateUserRequest request) {
        createAccount(request.email(), request.password(), request.role());
    }

    private void createAccount(String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setCreatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Same generic error whether the email is unknown or the password is wrong,
        // so an attacker can't tell which emails are registered.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return tokensFor(user);
    }

    /** Exchange a valid refresh token for a new access token (and a rotated refresh token). */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(
                rawRefreshToken,
                userId -> userRepository.findById(userId).orElse(null));
        User user = rotation.user();
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(accessToken, rotation.rawToken(), user.getEmail(), user.getRole().name());
    }

    /** Revoke a refresh token so it can no longer be used (logout). */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResponse tokensFor(User user) {
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getRole().name());
    }
}
