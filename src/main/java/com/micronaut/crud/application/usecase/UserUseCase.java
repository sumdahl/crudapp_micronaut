package com.micronaut.crud.application.usecase;

import com.micronaut.crud.application.dto.CreateUserRequest;
import com.micronaut.crud.application.dto.UpdateUserRequest;
import com.micronaut.crud.application.dto.UserDTO;
import com.micronaut.crud.domain.entity.User;
import com.micronaut.crud.domain.exception.DuplicateResourceException;
import com.micronaut.crud.domain.repository.UserRepository;
import com.micronaut.crud.infrastructure.mapper.UserMapper;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * User use case (service) - Application layer
 * Contains business logic for user operations
 */
@Singleton
public class UserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserUseCase(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        return userMapper.toDTO(savedUser);
    }

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO);
    }

    public Optional<UserDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toDTO);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<UserDTO> updateUser(Long id, UpdateUserRequest request) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setUsername(request.getUsername());
                    existingUser.setEmail(request.getEmail());
                    existingUser.setFirstName(request.getFirstName());
                    existingUser.setLastName(request.getLastName());

                    // Only update password if it's provided
                    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                        existingUser.setPassword(request.getPassword());
                    }

                    User updatedUser = userRepository.update(existingUser);
                    return userMapper.toDTO(updatedUser);
                });
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
