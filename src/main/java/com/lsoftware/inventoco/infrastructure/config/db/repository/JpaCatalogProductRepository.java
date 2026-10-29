package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.CatalogProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCatalogProductRepository extends JpaRepository<CatalogProductEntity, UUID> {
    Optional<CatalogProductEntity> findByGtin(String gtin);
    Optional<CatalogProductEntity> findFirstByNormalizedNameOrderByCreatedAt(String normalizedName);
    List<CatalogProductEntity> findByNormalizedNameContainingOrderByNormalizedName(String text, Pageable page);
}
