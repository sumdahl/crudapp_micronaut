package com.micronaut.crud.domain.repository;

import com.micronaut.crud.domain.entity.User;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User repository interface - Domain layer
 * Micronaut Data JPA will provide the implementation
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
