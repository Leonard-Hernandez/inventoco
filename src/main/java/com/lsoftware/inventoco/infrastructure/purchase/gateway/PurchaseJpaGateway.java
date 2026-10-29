package com.lsoftware.inventoco.infrastructure.purchase.gateway;

import com.lsoftware.inventoco.entity.purchase.gateway.PurchaseGateway;
import com.lsoftware.inventoco.entity.purchase.model.LineReason;
import com.lsoftware.inventoco.entity.purchase.model.Purchase;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseLine;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseStatus;
import com.lsoftware.inventoco.infrastructure.config.db.repository.JpaPurchaseRepository;
import com.lsoftware.inventoco.infrastructure.config.db.schema.PurchaseEntity;
import com.lsoftware.inventoco.infrastructure.config.db.schema.PurchaseLineEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseJpaGateway implements PurchaseGateway {

    private final JpaPurchaseRepository repo;

    @Override
    public Optional<Purchase> findDraft(UUID ownerId) {
        return repo.findByOwnerIdAndStatus(ownerId, PurchaseStatus.DRAFT.name()).map(PurchaseJpaGateway::toDomain);
    }

    @Override
    public Optional<Purchase> findOwned(UUID ownerId, UUID id) {
        return repo.findByIdAndOwnerId(id, ownerId).map(PurchaseJpaGateway::toDomain);
    }

    /** Lines load inside the caller's transaction (the lock needs one anyway). */
    @Override
    public Optional<Purchase> lockOwned(UUID ownerId, UUID id) {
        return repo.lockOwned(ownerId, id).map(PurchaseJpaGateway::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> listCompleted(UUID ownerId, int limit) {
        return repo.findByOwnerIdAndStatusOrderByCompletedAtDesc(ownerId, PurchaseStatus.COMPLETED.name(),
                PageRequest.of(0, limit)).stream().map(PurchaseJpaGateway::toDomain).toList();
    }

    /** Lines are written only when the purchase is new: a list is regenerated, never edited. */
    @Override
    public Purchase save(Purchase p) {
        PurchaseEntity e = p.id() == null ? new PurchaseEntity() : repo.findById(p.id()).orElseThrow();
        if (p.id() == null) {
            e.setId(UUID.randomUUID());
            e.setOwnerId(p.ownerId());
            e.setCurrency(p.currency());
            e.setHorizonDays(p.horizonDays());
            e.setCreatedAt(p.createdAt());
            for (PurchaseLine l : p.lines()) {
                e.getLines().add(PurchaseLineEntity.builder()
                        .id(UUID.randomUUID()).purchase(e).itemId(l.itemId()).position(l.position())
                        .reason(l.reason().name()).suggestedQuantity(l.suggestedQuantity())
                        .supplierId(l.supplierId()).supplierName(l.supplierName())
                        .bestUnitPrice(l.bestUnitPrice()).usualUnitPrice(l.usualUnitPrice())
                        .build());
            }
        }
        e.setStatus(p.status().name());
        e.setCompletedAt(p.completedAt());
        return toDomain(repo.save(e));
    }

    @Override
    public void delete(UUID id) {
        repo.deleteById(id);
        repo.flush();
    }

    static Purchase toDomain(PurchaseEntity e) {
        return new Purchase(e.getId(), e.getOwnerId(), PurchaseStatus.valueOf(e.getStatus()), e.getCurrency(),
                e.getHorizonDays(), e.getCreatedAt(), e.getCompletedAt(),
                e.getLines().stream().map(l -> new PurchaseLine(l.getId(), l.getItemId(), l.getPosition(),
                        LineReason.valueOf(l.getReason()), l.getSuggestedQuantity(), l.getSupplierId(),
                        l.getSupplierName(), l.getBestUnitPrice(), l.getUsualUnitPrice())).toList());
    }
}
