package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.PurchaseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPurchaseRepository extends JpaRepository<PurchaseEntity, UUID> {
    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseEntity> findByOwnerIdAndStatus(UUID ownerId, String status);

    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseEntity p where p.id = :id and p.ownerId = :owner")
    Optional<PurchaseEntity> lockOwned(@Param("owner") UUID owner, @Param("id") UUID id);

    List<PurchaseEntity> findByOwnerIdAndStatusOrderByCompletedAtDesc(UUID ownerId, String status, Pageable page);
}
