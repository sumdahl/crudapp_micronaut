package com.micronaut.crud.infrastructure.security;

import com.micronaut.crud.application.service.AuthService;
import com.micronaut.crud.domain.entity.User;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Optional;

@Singleton
public class AuthenticationProviderUserPassword implements HttpRequestAuthenticationProvider<Object> {

    private final AuthService authService;

    public AuthenticationProviderUserPassword(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthenticationResponse authenticate(HttpRequest<Object> httpRequest,
            AuthenticationRequest<String, String> authenticationRequest) {
        String email = authenticationRequest.getIdentity().toString();
        String password = authenticationRequest.getSecret().toString();

        // Verify credentials
        Optional<User> userOpt = authService.verifyCredentials(email, password);

        if (userOpt.isEmpty()) {
            return AuthenticationResponse.failure("Invalid credentials");
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.getActive()) {
            return AuthenticationResponse.failure("User account is inactive");
        }

        // Parse roles (comma-separated string to list)
        var roles = user.getRoles() != null
                ? Arrays.asList(user.getRoles().split(","))
                : Arrays.asList("USER");

        // Return successful authentication with user details
        return AuthenticationResponse.success(
                user.getEmail(),
                roles);
    }
}
