package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.PriceObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaPriceObservationRepository extends JpaRepository<PriceObservationEntity, UUID> {
    List<PriceObservationEntity> findByOwnerIdAndItemIdOrderByObservedAtDesc(UUID ownerId, UUID itemId);
    List<PriceObservationEntity> findByOwnerIdAndObservedAtGreaterThanEqualOrderByObservedAt(UUID ownerId, Instant since);

    @Query("""
            select p from PriceObservationEntity p
            where p.visibility = 'PUBLIC' and p.catalogProductId in :products
              and p.country = :country and p.currency = :currency and p.observedAt >= :since
            order by p.observedAt desc""")
    List<PriceObservationEntity> listPublic(@Param("products") Collection<UUID> products, @Param("country") String country,
                                            @Param("currency") String currency, @Param("since") Instant since);
}
