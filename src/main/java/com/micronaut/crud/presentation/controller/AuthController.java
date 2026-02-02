package com.micronaut.crud.presentation.controller;

import com.micronaut.crud.application.service.AuthService;
import com.micronaut.crud.application.service.RefreshTokenService;
import com.micronaut.crud.application.utils.JwtUtils;
import com.micronaut.crud.domain.entity.User;
import com.micronaut.crud.domain.exception.ResourceNotFoundException;
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

    @Post("/register")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<UserResponse> register(@Valid @Body RegisterRequest request) {
        UserResponse user = authService.register(request);
        return HttpResponse.created(user);
    }

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<?> login(@Valid @Body LoginRequest request) {
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

        // Safely get UserResponse from AuthService
        UserResponse userResponse = authService.getUserById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId().toString()));

        // Generate access token
        Optional<String> accessTokenOpt = tokenGenerator.generateToken(JwtUtils.buildClaims(userResponse));
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
        String refreshToken = refreshTokenService.generateAndStore(user.getId());

        Cookie refreshCookie = Cookie.of(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // true in prod
                .sameSite(io.micronaut.http.cookie.SameSite.Strict)
                .maxAge(Duration.ofDays(REFRESH_COOKIE_MAX_AGE_DAYS))
                .path("/");

        AuthResponse authResponse = new AuthResponse(accessToken, userResponse);

        return HttpResponse.ok(authResponse).cookie(refreshCookie);
    }

    @Post("/refresh")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Map<String, String>> refresh(
            @CookieValue(REFRESH_COOKIE_NAME) Optional<String> refreshTokenOpt) {

        if (refreshTokenOpt.isEmpty() || refreshTokenOpt.get().isBlank()) {
            return HttpResponse.unauthorized();
        }

        Optional<RefreshResult> refreshResultOpt = refreshTokenService.validateAndRotateWithUser(refreshTokenOpt.get());
        if (refreshResultOpt.isEmpty()) {
            return HttpResponse.unauthorized();
        }

        RefreshResult refreshResult = refreshResultOpt.get();
        UUID userId = refreshResult.getUserID();
        String newRefreshToken = refreshResult.getNewRefreshToken();

        UserResponse user = authService.getUserById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        Optional<String> accessTokenOpt = tokenGenerator.generateToken(JwtUtils.buildClaims(user));
        if (accessTokenOpt.isEmpty()) {
            return HttpResponse.serverError();
        }

        Cookie refreshCookie = Cookie.of(REFRESH_COOKIE_NAME, newRefreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite(io.micronaut.http.cookie.SameSite.Strict)
                .maxAge(Duration.ofDays(REFRESH_COOKIE_MAX_AGE_DAYS))
                .path("/");

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessTokenOpt.get());
        response.put("tokenType", "Bearer");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());

        return HttpResponse.ok(response).cookie(refreshCookie);
    }

    @Post("/logout")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Map<String, String>> logout(Authentication authentication) {
        String userIdStr = authentication.getAttributes().get("userId").toString();
        UUID userId = UUID.fromString(userIdStr);

        refreshTokenService.revokeAllForUser(userId);

        Cookie clearCookie = Cookie.of(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // true in prod
                .maxAge(Duration.ZERO)
                .path("/");

        return HttpResponse.ok(Map.of("message", "Logged out successfully")).cookie(clearCookie);
    }

    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<UserResponse> getCurrentUser(Authentication authentication) {
        return authService.getUserByEmail(authentication.getName())
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

}
