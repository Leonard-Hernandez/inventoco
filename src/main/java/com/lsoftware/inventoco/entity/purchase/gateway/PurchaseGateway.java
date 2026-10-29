package com.lsoftware.inventoco.entity.purchase.gateway;

import com.lsoftware.inventoco.entity.purchase.model.Purchase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseGateway {
    Optional<Purchase> findDraft(UUID ownerId);
    Optional<Purchase> findOwned(UUID ownerId, UUID id);
    List<Purchase> listCompleted(UUID ownerId, int limit);
    Purchase save(Purchase purchase);
    void delete(UUID id);
}
