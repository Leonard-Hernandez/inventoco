package com.lsoftware.inventoco.entity.supplier.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Where something is bought. A user's own suppliers have an owner; system suppliers (store
 * chains whose prices are seeded for everyone) have none and every user can pick them.
 */
public record Supplier(
        UUID id,
        UUID ownerId,
        String name,
        String country,
        String city,
        String url,
        boolean archived,
        Instant createdAt
) {

    public boolean system() {
        return ownerId == null;
    }

    public boolean usableBy(UUID userId) {
        return !archived && (system() || ownerId.equals(userId));
    }

    public Supplier withDetails(String name, String country, String city, String url) {
        return new Supplier(id, ownerId, name, country, city, url, archived, createdAt);
    }

    public Supplier archive() {
        return new Supplier(id, ownerId, name, country, city, url, true, createdAt);
    }
}
