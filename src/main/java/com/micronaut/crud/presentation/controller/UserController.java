package com.micronaut.crud.presentation.controller;

import com.micronaut.crud.application.dto.CreateUserRequest;
import com.micronaut.crud.application.dto.UpdateUserRequest;
import com.micronaut.crud.application.dto.UserDTO;
import com.micronaut.crud.application.usecase.UserUseCase;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@Controller("/api/users")
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
    public HttpResponse<UserDTO> getUserById(@PathVariable UUID id) {
        return userUseCase.getUserById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get("/username/{username}")
    public HttpResponse<UserDTO> getUserByUsername(@PathVariable String username) {
        return userUseCase.getUserByUsername(username)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get
    public HttpResponse<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userUseCase.getAllUsers();
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
