package com.lsoftware.inventoco.entity.price.gateway;

import com.lsoftware.inventoco.entity.price.model.PriceObservation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PriceGateway {
    PriceObservation save(PriceObservation price);
    /** Owner's prices of one item, newest first. */
    List<PriceObservation> listByItem(UUID ownerId, UUID itemId);
    /** Owner's prices of every item since an instant. */
    List<PriceObservation> listByOwnerSince(UUID ownerId, Instant since);
    /** PUBLIC prices of catalog products in one country and currency since an instant, newest first. */
    List<PriceObservation> listPublic(Collection<UUID> catalogProductIds, String country, String currency, Instant since);
}
