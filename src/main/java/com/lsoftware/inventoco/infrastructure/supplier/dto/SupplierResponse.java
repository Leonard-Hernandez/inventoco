package com.lsoftware.inventoco.infrastructure.supplier.dto;

import com.lsoftware.inventoco.entity.supplier.model.Supplier;

import java.util.UUID;

/** storeChain: a system supplier shared by everyone (read-only). */
public record SupplierResponse(UUID id, String name, String country, String city, String url, boolean storeChain) {

    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(s.id(), s.name(), s.country(), s.city(), s.url(), s.system());
    }
}
