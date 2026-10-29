package com.lsoftware.inventoco.entity.supplier.gateway;

import com.lsoftware.inventoco.entity.supplier.model.Supplier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierGateway {
    Optional<Supplier> findById(UUID id);
    List<Supplier> findAllById(Collection<UUID> ids);
    /** The owner's active suppliers plus the system ones of a country. */
    List<Supplier> listAvailable(UUID ownerId, String country);
    /** A store chain (system supplier) of a country, by name ignoring case. */
    Optional<Supplier> findSystem(String country, String name);
    Supplier save(Supplier supplier);
}
