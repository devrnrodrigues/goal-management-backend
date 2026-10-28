package com.devrenanrodrigues.goalmanagementapi.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM UserEntity u
        WHERE (:search IS NULL OR :search = ''
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY u.name ASC
    """)
    org.springframework.data.domain.Page<UserEntity> searchUsers(
        @org.springframework.data.repository.query.Param("search") String search,
        org.springframework.data.domain.Pageable pageable
    );
}
