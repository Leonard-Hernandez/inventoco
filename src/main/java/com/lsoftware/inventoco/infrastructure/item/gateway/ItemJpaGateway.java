package com.lsoftware.inventoco.infrastructure.item.gateway;

import com.lsoftware.inventoco.entity.item.gateway.ItemGateway;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.infrastructure.config.db.repository.JpaItemRepository;
import com.lsoftware.inventoco.infrastructure.config.db.schema.ItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItemJpaGateway implements ItemGateway {

    private final JpaItemRepository repo;
    private final Clock clock;

    @Override
    public Optional<Item> findOwned(UUID ownerId, UUID id) {
        return repo.findByIdAndOwnerId(id, ownerId).map(ItemJpaGateway::toDomain);
    }

    @Override
    public List<Item> listActive(UUID ownerId, String query) {
        List<ItemEntity> list = query == null
                ? repo.findByOwnerIdAndArchivedFalseOrderByName(ownerId)
                : repo.search(ownerId, "%" + query.toLowerCase(Locale.ROOT) + "%", query);
        return list.stream().map(ItemJpaGateway::toDomain).toList();
    }

    @Override
    public List<Item> findOwned(UUID ownerId, Collection<UUID> ids) {
        return repo.findByOwnerIdAndIdIn(ownerId, ids).stream().map(ItemJpaGateway::toDomain).toList();
    }

    /** Never writes current_stock of an existing item: only addStock changes it. */
    @Override
    public Item save(Item i) {
        ItemEntity e = i.id() == null ? new ItemEntity() : repo.findById(i.id()).orElseThrow();
        if (i.id() == null) {
            e.setId(UUID.randomUUID());
            e.setOwnerId(i.ownerId());
            e.setDimension(i.dimension().name());
            e.setCurrentStock(i.currentStock());
            e.setCreatedAt(i.createdAt());
        }
        e.setName(i.name());
        e.setCategory(i.category());
        e.setDisplayUnit(i.displayUnit().name());
        e.setMinStock(i.minStock());
        e.setBarcode(i.barcode());
        e.setCatalogProductId(i.catalogProductId());
        e.setArchived(i.archived());
        e.setUpdatedAt(i.updatedAt());
        return toDomain(repo.save(e));
    }

    @Override
    public boolean addStock(UUID ownerId, UUID id, BigDecimal delta) {
        return repo.addStock(ownerId, id, delta, clock.instant()) == 1;
    }

    public static Item toDomain(ItemEntity e) {
        return new Item(e.getId(), e.getOwnerId(), e.getName(), e.getCategory(), Dimension.valueOf(e.getDimension()),
                Unit.valueOf(e.getDisplayUnit()), e.getMinStock(), e.getCurrentStock(), e.getBarcode(),
                e.getCatalogProductId(), e.isArchived(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
