package com.micronaut.crud.presentation.controller;

import com.micronaut.crud.application.dto.CreateUserRequest;
import com.micronaut.crud.application.dto.UserDTO;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for UserController
 */
@MicronautTest
class UserControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testGetAllUsersEndpoint() {
        // Test that the endpoint is accessible
        HttpRequest<?> request = HttpRequest.GET("/api/users");
        var response = client.toBlocking().exchange(request);

        assertEquals(HttpStatus.OK, response.status());
    }

    @Test
    void testCreateUserValidation() {
        // Test with invalid data
        CreateUserRequest invalidRequest = new CreateUserRequest();
        invalidRequest.setUsername("ab"); // Too short
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("123"); // Too short

        HttpRequest<?> request = HttpRequest.POST("/api/users", invalidRequest);

        try {
            client.toBlocking().exchange(request, UserDTO.class);
            fail("Expected validation to fail");
        } catch (Exception e) {
            // Expected exception due to validation
            assertTrue(true);
        }
    }
}
