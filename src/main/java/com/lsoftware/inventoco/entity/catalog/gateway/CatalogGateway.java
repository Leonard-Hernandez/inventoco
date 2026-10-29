package com.lsoftware.inventoco.entity.catalog.gateway;

import com.lsoftware.inventoco.entity.catalog.model.CatalogProduct;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogGateway {
    Optional<CatalogProduct> findById(UUID id);
    Optional<CatalogProduct> findByGtin(String gtin);
    Optional<CatalogProduct> findByNormalizedName(String normalizedName);
    List<CatalogProduct> search(String normalizedQuery, int limit);
    CatalogProduct save(CatalogProduct product);
}
