package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.StockMovementEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaStockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {
    List<StockMovementEntity> findByOwnerIdAndItemIdOrderByOccurredAtDescCreatedAtDesc(UUID ownerId, UUID itemId, Pageable page);
    List<StockMovementEntity> findByOwnerIdAndTypeInAndOccurredAtGreaterThanEqualOrderByOccurredAt(
            UUID ownerId, Collection<String> types, Instant since);
}
