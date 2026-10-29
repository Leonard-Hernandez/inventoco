package com.lsoftware.inventoco.entity.item.gateway;

import com.lsoftware.inventoco.entity.item.model.Item;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemGateway {
    /** Only the owner's items: another user's id is the same as a missing one. */
    Optional<Item> findOwned(UUID ownerId, UUID id);
    List<Item> listActive(UUID ownerId, String query);
    List<Item> findOwned(UUID ownerId, Collection<UUID> ids);
    Item save(Item item);

    /**
     * Adds delta to the stock in one statement, so concurrent movements never lose an update.
     * Returns false (and changes nothing) if the stock would go below zero.
     */
    boolean addStock(UUID ownerId, UUID id, BigDecimal delta);
}
