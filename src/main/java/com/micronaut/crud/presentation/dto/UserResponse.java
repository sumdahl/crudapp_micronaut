package com.micronaut.crud.presentation.dto;

import java.util.UUID;

import io.micronaut.serde.annotation.Serdeable;

/**
 * DTO for user response (without sensitive data)
 */

@Serdeable
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String roles;
    private String firstName;
    private String lastName;
    private Boolean active;

    // Constructors
    public UserResponse() {
    }

    public UserResponse(UUID id, String username, String email, String roles, String firstName, String lastName,
            Boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
