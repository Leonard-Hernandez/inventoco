package com.lsoftware.inventoco.infrastructure.catalog.gateway;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogGateway;
import com.lsoftware.inventoco.entity.catalog.model.CatalogProduct;
import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.infrastructure.config.db.repository.JpaCatalogProductRepository;
import com.lsoftware.inventoco.infrastructure.config.db.schema.CatalogProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogJpaGateway implements CatalogGateway {

    private final JpaCatalogProductRepository repo;

    @Override
    public Optional<CatalogProduct> findById(UUID id) {
        return repo.findById(id).map(CatalogJpaGateway::toDomain);
    }

    @Override
    public Optional<CatalogProduct> findByGtin(String gtin) {
        return repo.findByGtin(gtin).map(CatalogJpaGateway::toDomain);
    }

    @Override
    public Optional<CatalogProduct> findByNormalizedName(String normalizedName) {
        return repo.findFirstByNormalizedNameOrderByCreatedAt(normalizedName).map(CatalogJpaGateway::toDomain);
    }

    @Override
    public List<CatalogProduct> search(String normalizedQuery, int limit) {
        return repo.findByNormalizedNameContainingOrderByNormalizedName(normalizedQuery, PageRequest.of(0, limit))
                .stream().map(CatalogJpaGateway::toDomain).toList();
    }

    @Override
    public CatalogProduct save(CatalogProduct p) {
        return toDomain(repo.save(CatalogProductEntity.builder()
                .id(p.id() != null ? p.id() : UUID.randomUUID())
                .name(p.name()).normalizedName(p.normalizedName()).brand(p.brand()).gtin(p.gtin())
                .dimension(p.dimension().name()).createdAt(p.createdAt())
                .build()));
    }

    static CatalogProduct toDomain(CatalogProductEntity e) {
        return new CatalogProduct(e.getId(), e.getName(), e.getNormalizedName(), e.getBrand(), e.getGtin(),
                Dimension.valueOf(e.getDimension()), e.getCreatedAt());
    }
}
