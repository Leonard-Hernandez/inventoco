package com.lsoftware.inventoco.usecase.item;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.item.exception.ItemNotFoundException;
import com.lsoftware.inventoco.entity.item.gateway.ItemGateway;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.usecase.catalog.LinkCatalogProductUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The user's items. The dimension is fixed at creation: the stored quantities depend on it. */
@Component
@RequiredArgsConstructor
public class ManageItemUseCase {

    private final ItemGateway items;
    private final LinkCatalogProductUseCase catalog;
    private final Clock clock;

    /**
     * @param unit     display unit; also the unit of minStock and initialStock
     */
    public record ItemCommand(String name, String category, String unit, BigDecimal minStock, BigDecimal initialStock,
                              String barcode, UUID catalogProductId) {}

    @Transactional
    public Item create(UUID ownerId, ItemCommand c) {
        Unit unit = Unit.parse(c.unit());
        Dimension dimension = unit.dimension();
        String name = name(c.name());
        Instant now = clock.instant();
        UUID productId = catalog.resolve(c.catalogProductId(), c.barcode(), name, dimension);
        return items.save(new Item(null, ownerId, name, blankToNull(c.category()), dimension, unit,
                quantity(unit, c.minStock()), quantity(unit, c.initialStock()), blankToNull(c.barcode()), productId,
                false, now, now));
    }

    @Transactional
    public Item update(UUID ownerId, UUID id, ItemCommand c) {
        Item item = get(ownerId, id);
        Unit unit = Unit.parse(c.unit(), item.dimension());
        String name = name(c.name());
        UUID productId = catalog.resolve(c.catalogProductId(), c.barcode(), name, item.dimension());
        return items.save(item.withDetails(name, blankToNull(c.category()), unit, quantity(unit, c.minStock()),
                blankToNull(c.barcode()), productId, clock.instant()));
    }

    /** Archived items keep their history (prices, movements) but leave lists and forecasts. */
    @Transactional
    public void archive(UUID ownerId, UUID id) {
        items.save(get(ownerId, id).archivedAt(clock.instant()));
    }

    public Item get(UUID ownerId, UUID id) {
        return items.findOwned(ownerId, id).filter(i -> !i.archived()).orElseThrow(ItemNotFoundException::new);
    }

    public List<Item> list(UUID ownerId, String query, boolean lowStockOnly) {
        return items.listActive(ownerId, blankToNull(query)).stream()
                .filter(i -> !lowStockOnly || i.lowStock())
                .toList();
    }

    /** Owned items by id, archived ones included (old shopping lists may name them). */
    public java.util.Map<UUID, Item> byIds(UUID ownerId, java.util.Collection<UUID> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        return items.findOwned(ownerId, ids).stream()
                .collect(java.util.stream.Collectors.toMap(Item::id, java.util.function.Function.identity()));
    }

    private static BigDecimal quantity(Unit unit, BigDecimal value) {
        if (value == null) return BigDecimal.ZERO.setScale(Unit.SCALE);
        if (value.signum() < 0) throw new DomainException("INVALID_QUANTITY", "Quantities cannot be negative");
        return unit.toBase(value);
    }

    private static String name(String name) {
        if (name == null || name.isBlank()) throw new DomainException("INVALID_NAME", "The item needs a name");
        return name.strip();
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
