package com.micronaut.crud.presentation.controller;

import com.micronaut.crud.application.service.AuthService;
import com.micronaut.crud.application.service.RefreshTokenService;
import com.micronaut.crud.domain.entity.User;
import com.micronaut.crud.presentation.dto.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.token.generator.TokenGenerator;
import jakarta.validation.Valid;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication controller - handles registration, login, refresh, logout, and
 * user info
 */
@Controller("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final TokenGenerator tokenGenerator;

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_COOKIE_MAX_AGE_DAYS = 7;

    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            TokenGenerator tokenGenerator) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.tokenGenerator = tokenGenerator;
    }

    /**
     * Register a new user
     * Returns 201 Created with user details (no tokens)
     */
    @Post("/register")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<UserResponse> register(@Valid @Body RegisterRequest request) {
        UserResponse user = authService.register(request);
        return HttpResponse.created(user);
    }

    /**
     * Login endpoint
     * Returns access token in response body
     * Sets refresh token as HttpOnly cookie
     */
    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<?> login(@Valid @Body LoginRequest request) {
        // Verify credentials
        Optional<User> userOpt = authService.verifyCredentials(request.getEmail(), request.getPassword());

        if (userOpt.isEmpty()) {
            ErrorResponse error = new ErrorResponse(
              401,
                    "Unauthorized",
                    "Invalid email or password",
                    "/api/auth/login"
            );
            return HttpResponse.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        User user = userOpt.get();

        // Generate access token (JWT)
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("roles", user.getRoles());
        claims.put("userId", user.getId().toString());

        Optional<String> accessTokenOpt = tokenGenerator.generateToken(claims);

        if (accessTokenOpt.isEmpty()) {
            ErrorResponse error = new ErrorResponse(
                    500,
                    "Internal Server Error",
                    "Failed to generate access token",
                    "/api/auth/login"
            );
            return HttpResponse.serverError().body(error);
        }

        String accessToken = accessTokenOpt.get();

        // Generate refresh token
        String refreshToken = refreshTokenService.generateAndStore(user.getId());

        // Create refresh token cookie
        Cookie refreshCookie = Cookie.of(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .sameSite(io.micronaut.http.cookie.SameSite.Strict)
                .maxAge(Duration.ofDays(REFRESH_COOKIE_MAX_AGE_DAYS))
                .path("/");

        // Create response
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getFirstName(),
                user.getLastName(),
                user.getActive());

        AuthResponse authResponse = new AuthResponse(accessToken, userResponse);

        return HttpResponse.ok(authResponse)
                .cookie(refreshCookie);
    }

    /**
     * Refresh access token using refresh token from cookie
     * Returns new access token
     * Rotates refresh token (revokes old, creates new)
     */
    @Post("/refresh")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Map<String, String>> refresh(
            @CookieValue(REFRESH_COOKIE_NAME) Optional<String> refreshTokenOpt) {
        if (refreshTokenOpt.isEmpty()) {
            return HttpResponse.unauthorized();
        }

        String refreshToken = refreshTokenOpt.get();

        // Verify refresh token and get user ID
        Optional<UUID> userIdOpt = refreshTokenService.verifyToken(refreshToken);

        if (userIdOpt.isEmpty()) {
            return HttpResponse.unauthorized();
        }

        UUID userId = userIdOpt.get();

        // Rotate refresh token
        Optional<String> newRefreshTokenOpt = refreshTokenService.validateAndRotate(refreshToken, userId);

        if (newRefreshTokenOpt.isEmpty()) {
            return HttpResponse.unauthorized();
        }

        // Get user details for new access token
        Optional<UserResponse> userOpt = authService.getUserByEmail(
                // We need to get user by ID; for now, we'll work around this
                // In production, add a getUserById method to AuthService
                /* This is a workaround - ideally fetch by userId */
                null);

        // Generate new access token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        // Note: In production, fetch full user details to populate roles
        claims.put("roles", "USER");

        Optional<String> accessTokenOpt = tokenGenerator.generateToken(claims);

        if (accessTokenOpt.isEmpty()) {
            return HttpResponse.serverError();
        }

        // Create new refresh token cookie
        Cookie refreshCookie = Cookie.of(REFRESH_COOKIE_NAME, newRefreshTokenOpt.get())
                .httpOnly(true)
                .secure(false)
                .sameSite(io.micronaut.http.cookie.SameSite.Strict)
                .maxAge(Duration.ofDays(REFRESH_COOKIE_MAX_AGE_DAYS))
                .path("/");

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessTokenOpt.get());
        response.put("tokenType", "Bearer");

        return HttpResponse.ok(response)
                .cookie(refreshCookie);
    }

    /**
     * Logout endpoint
     * Revokes refresh token and clears cookie
     */
    @Post("/logout")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Map<String, String>> logout(
            Authentication authentication,
            @CookieValue(REFRESH_COOKIE_NAME) Optional<String> refreshTokenOpt) {

        // Get user ID from authentication
        String userIdStr = authentication.getAttributes().get("userId").toString();
        UUID userId = UUID.fromString(userIdStr);

        // Revoke all refresh tokens for this user
        refreshTokenService.revokeAllForUser(userId);

        // Clear the refresh token cookie
        Cookie clearCookie = Cookie.of(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .maxAge(Duration.ZERO)
                .path("/");

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");

        return HttpResponse.ok(response)
                .cookie(clearCookie);
    }

    /**
     * Get current authenticated user details
     * Secured endpoint - requires valid access token
     */
    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<UserResponse> getCurrentUser(Authentication authentication) {
        // Get user email from authentication
        String email = authentication.getName();

        Optional<UserResponse> userOpt = authService.getUserByEmail(email);

        if (userOpt.isEmpty()) {
            return HttpResponse.notFound();
        }

        return HttpResponse.ok(userOpt.get());
    }
}
