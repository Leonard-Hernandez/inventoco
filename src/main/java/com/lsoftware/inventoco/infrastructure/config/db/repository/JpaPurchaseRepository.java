package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.PurchaseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPurchaseRepository extends JpaRepository<PurchaseEntity, UUID> {
    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseEntity> findByOwnerIdAndStatus(UUID ownerId, String status);

    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<PurchaseEntity> findByOwnerIdAndStatusOrderByCompletedAtDesc(UUID ownerId, String status, Pageable page);
}
