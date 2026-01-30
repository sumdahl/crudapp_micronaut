package com.micronaut.crud.domain.repository;

import com.micronaut.crud.domain.entity.User;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * User repository interface - Domain layer
 * Micronaut Data will automatically implement this interface
 */
@Repository
public interface UserRepository extends CrudRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
