package com.lsoftware.inventoco.usecase.item;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.item.exception.InsufficientStockException;
import com.lsoftware.inventoco.entity.item.gateway.ItemGateway;
import com.lsoftware.inventoco.entity.item.gateway.MovementGateway;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.entity.item.model.StockMovement;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Stock changes. The movement and the item's stock change in the same transaction; the stock is
 * updated with one atomic statement so two devices recording at once never lose a change.
 */
@Component
@RequiredArgsConstructor
public class RecordMovementUseCase {

    private final ItemGateway items;
    private final MovementGateway movements;
    private final ManageItemUseCase itemQueries;
    private final Clock clock;

    /**
     * @param quantity   how much moved, in unit; for ADJUSTMENT the counted stock
     * @param occurredAt when it happened (defaults to now; future instants are rejected)
     */
    public record MovementCommand(MovementType type, BigDecimal quantity, String unit, Instant occurredAt,
                                  String note) {}

    @Transactional
    public StockMovement execute(UUID ownerId, UUID itemId, MovementCommand c) {
        return record(ownerId, itemQueries.get(ownerId, itemId), c, null);
    }

    /** Also used when a shopping list is completed (purchaseId links the movement to it). */
    @Transactional
    public StockMovement record(UUID ownerId, Item item, MovementCommand c, UUID purchaseId) {
        if (c.type() == null) throw new DomainException("INVALID_MOVEMENT", "Missing movement type");
        if (c.quantity() == null || c.quantity().signum() < 0) {
            throw new DomainException("INVALID_QUANTITY", "Quantities cannot be negative");
        }
        Unit unit = c.unit() == null || c.unit().isBlank() ? item.displayUnit() : Unit.parse(c.unit(), item.dimension());
        BigDecimal amount = unit.toBase(c.quantity());
        BigDecimal delta = switch (c.type()) {
            case PURCHASE -> amount;
            case CONSUMPTION, WASTE -> amount.negate();
            case ADJUSTMENT -> amount.subtract(item.currentStock());
        };
        if (delta.signum() == 0 && c.type() != MovementType.ADJUSTMENT) {
            throw new DomainException("INVALID_QUANTITY", "The quantity must be greater than zero");
        }
        Instant now = clock.instant();
        Instant occurredAt = c.occurredAt() == null ? now : c.occurredAt();
        if (occurredAt.isAfter(now.plusSeconds(300))) {
            throw new DomainException("INVALID_DATE", "Movements cannot happen in the future");
        }
        if (!items.addStock(ownerId, item.id(), delta)) {
            throw new InsufficientStockException(item.displayUnit().fromBase(item.currentStock()).toPlainString()
                    + " " + item.displayUnit().symbol());
        }
        String note = c.note() == null || c.note().isBlank() ? null : c.note().strip();
        return movements.save(new StockMovement(null, ownerId, item.id(), c.type(), delta, occurredAt, note,
                purchaseId, now));
    }

    public List<StockMovement> history(UUID ownerId, UUID itemId, int limit) {
        itemQueries.get(ownerId, itemId);
        return movements.listByItem(ownerId, itemId, Math.clamp(limit, 1, 500));
    }
}
