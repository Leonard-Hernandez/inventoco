package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByAuthcoSub(String authcoSub);
}
