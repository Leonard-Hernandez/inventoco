package com.lsoftware.inventoco.infrastructure.price.gateway;

import com.lsoftware.inventoco.entity.price.gateway.PriceGateway;
import com.lsoftware.inventoco.entity.price.model.PriceObservation;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.infrastructure.config.db.repository.JpaPriceObservationRepository;
import com.lsoftware.inventoco.infrastructure.config.db.schema.PriceObservationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PriceJpaGateway implements PriceGateway {

    private final JpaPriceObservationRepository repo;

    @Override
    public PriceObservation save(PriceObservation p) {
        return toDomain(repo.save(PriceObservationEntity.builder()
                .id(p.id() != null ? p.id() : UUID.randomUUID())
                .ownerId(p.ownerId()).itemId(p.itemId()).supplierId(p.supplierId())
                .catalogProductId(p.catalogProductId()).amount(p.amount()).currency(p.currency())
                .packageQuantity(p.packageQuantity()).packageUnit(p.packageUnit().name()).unitPrice(p.unitPrice())
                .country(p.country()).visibility(p.visibility().name()).observedAt(p.observedAt())
                .purchaseId(p.purchaseId()).createdAt(p.createdAt())
                .build()));
    }

    @Override
    public List<PriceObservation> listByItem(UUID ownerId, UUID itemId) {
        return repo.findByOwnerIdAndItemIdOrderByObservedAtDesc(ownerId, itemId).stream()
                .map(PriceJpaGateway::toDomain).toList();
    }

    @Override
    public List<PriceObservation> listByOwnerSince(UUID ownerId, Instant since) {
        return repo.findByOwnerIdAndObservedAtGreaterThanEqualOrderByObservedAt(ownerId, since).stream()
                .map(PriceJpaGateway::toDomain).toList();
    }

    @Override
    public List<PriceObservation> listPublic(Collection<UUID> catalogProductIds, String country, String currency,
                                             Instant since) {
        return repo.listPublic(catalogProductIds, country, currency, since).stream()
                .map(PriceJpaGateway::toDomain).toList();
    }

    static PriceObservation toDomain(PriceObservationEntity e) {
        return new PriceObservation(e.getId(), e.getOwnerId(), e.getItemId(), e.getSupplierId(), e.getCatalogProductId(),
                e.getAmount(), e.getCurrency(), e.getPackageQuantity(), Unit.valueOf(e.getPackageUnit()),
                e.getUnitPrice(), e.getCountry(), Visibility.valueOf(e.getVisibility()), e.getObservedAt(),
                e.getPurchaseId(), e.getCreatedAt());
    }
}
