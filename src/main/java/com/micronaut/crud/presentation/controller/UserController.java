package com.micronaut.crud.presentation.controller;

import com.micronaut.crud.application.dto.CreateUserRequest;
import com.micronaut.crud.application.dto.UpdateUserRequest;
import com.micronaut.crud.application.dto.UserDTO;
import com.micronaut.crud.application.usecase.UserUseCase;

import io.micronaut.data.model.Page;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;

import io.micronaut.data.model.Pageable;

import java.util.UUID;

@Controller("/api/users")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @Post
    @Status(HttpStatus.CREATED)
    public HttpResponse<UserDTO> createUser(@Valid @Body CreateUserRequest request) {
        UserDTO createdUser = userUseCase.createUser(request);
        return HttpResponse.created(createdUser);
    }

    @Get("/{id}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<UserDTO> getUserById(@PathVariable UUID id) {
        return userUseCase.getUserById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get("/username/{username}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<UserDTO> getUserByUsername(@PathVariable String username) {
        return userUseCase.getUserByUsername(username)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get
    public HttpResponse<Page<UserDTO>> getAllUsers(Pageable pageable) {
        Page<UserDTO> users = userUseCase.getAllUsers(pageable);
        return HttpResponse.ok(users);
    }

    @Put("/{id}")
    public HttpResponse<UserDTO> updateUser(@PathVariable UUID id, @Valid @Body UpdateUserRequest request) {
        return userUseCase.updateUser(id, request)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    public HttpResponse<Void> deleteUser(@PathVariable UUID id) {
        boolean deleted = userUseCase.deleteUser(id);
        return deleted ? HttpResponse.noContent() : HttpResponse.notFound();
    }
}
