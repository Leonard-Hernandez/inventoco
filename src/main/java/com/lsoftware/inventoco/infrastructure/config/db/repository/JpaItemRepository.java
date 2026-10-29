package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.ItemEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaItemRepository extends JpaRepository<ItemEntity, UUID> {
    Optional<ItemEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemEntity i where i.id = :id and i.ownerId = :owner")
    Optional<ItemEntity> lockOwned(@Param("owner") UUID owner, @Param("id") UUID id);
    List<ItemEntity> findByOwnerIdAndIdIn(UUID ownerId, Collection<UUID> ids);
    List<ItemEntity> findByOwnerIdAndArchivedFalseOrderByName(UUID ownerId);

    @Query("""
            select i from ItemEntity i
            where i.ownerId = :owner and i.archived = false
              and (lower(i.name) like :text or lower(coalesce(i.category, '')) like :text or i.barcode = :raw)
            order by i.name""")
    List<ItemEntity> search(@Param("owner") UUID owner, @Param("text") String text, @Param("raw") String raw);

    /** Atomic: the stock never goes below zero and concurrent movements never overwrite each other. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ItemEntity i set i.currentStock = i.currentStock + :delta, i.updatedAt = :now
            where i.id = :id and i.ownerId = :owner and i.currentStock + :delta >= 0""")
    int addStock(@Param("owner") UUID owner, @Param("id") UUID id, @Param("delta") BigDecimal delta,
                 @Param("now") Instant now);
}
