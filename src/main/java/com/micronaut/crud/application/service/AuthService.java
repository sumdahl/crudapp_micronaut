package com.micronaut.crud.application.service;

import com.micronaut.crud.domain.entity.User;
import com.micronaut.crud.domain.exception.DuplicateResourceException;
import com.micronaut.crud.domain.repository.UserRepository;
import com.micronaut.crud.presentation.dto.RegisterRequest;
import com.micronaut.crud.presentation.dto.UserResponse;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.Optional;

@Singleton
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHashService passwordHashService;

    public AuthService(UserRepository userRepository, PasswordHashService passwordHashService) {
        this.userRepository = userRepository;
        this.passwordHashService = passwordHashService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHashService.hash(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoles("USER");
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToUserResponse);
    }

    public Optional<User> verifyCredentials(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();

        if (passwordHashService.verify(password, user.getPasswordHash())) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getFirstName(),
                user.getLastName(),
                user.getActive());
    }
}
