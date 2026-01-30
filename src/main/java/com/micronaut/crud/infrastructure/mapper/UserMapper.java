package com.micronaut.crud.infrastructure.mapper;

import com.micronaut.crud.application.dto.CreateUserRequest;
import com.micronaut.crud.application.dto.UserDTO;
import com.micronaut.crud.domain.entity.User;
import jakarta.inject.Singleton;

/**
 * Mapper class to convert between User entity and DTOs - Infrastructure layer
 */
@Singleton
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public User toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return user;
    }
}
