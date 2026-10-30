package com.library.auth;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StaffUserRepository extends MongoRepository<StaffUser, String> {
    Optional<StaffUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
