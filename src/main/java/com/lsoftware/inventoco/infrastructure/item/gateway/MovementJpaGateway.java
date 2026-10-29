package com.lsoftware.inventoco.infrastructure.item.gateway;

import com.lsoftware.inventoco.entity.item.gateway.MovementGateway;
import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.entity.item.model.StockMovement;
import com.lsoftware.inventoco.infrastructure.config.db.repository.JpaStockMovementRepository;
import com.lsoftware.inventoco.infrastructure.config.db.schema.StockMovementEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MovementJpaGateway implements MovementGateway {

    private static final List<String> OUTFLOWS = List.of(MovementType.CONSUMPTION.name(), MovementType.WASTE.name());

    private final JpaStockMovementRepository repo;

    @Override
    public StockMovement save(StockMovement m) {
        return toDomain(repo.save(StockMovementEntity.builder()
                .id(m.id() != null ? m.id() : UUID.randomUUID())
                .ownerId(m.ownerId()).itemId(m.itemId()).type(m.type().name()).quantity(m.quantity())
                .occurredAt(m.occurredAt()).note(m.note()).purchaseId(m.purchaseId()).createdAt(m.createdAt())
                .build()));
    }

    @Override
    public List<StockMovement> listByItem(UUID ownerId, UUID itemId, int limit) {
        return repo.findByOwnerIdAndItemIdOrderByOccurredAtDescCreatedAtDesc(ownerId, itemId, PageRequest.of(0, limit))
                .stream().map(MovementJpaGateway::toDomain).toList();
    }

    @Override
    public List<StockMovement> outflowsSince(UUID ownerId, Instant since) {
        return repo.findByOwnerIdAndTypeInAndOccurredAtGreaterThanEqualOrderByOccurredAt(ownerId, OUTFLOWS, since)
                .stream().map(MovementJpaGateway::toDomain).toList();
    }

    static StockMovement toDomain(StockMovementEntity e) {
        return new StockMovement(e.getId(), e.getOwnerId(), e.getItemId(), MovementType.valueOf(e.getType()),
                e.getQuantity(), e.getOccurredAt(), e.getNote(), e.getPurchaseId(), e.getCreatedAt());
    }
}
