package com.lsoftware.inventoco.entity.catalog.model;

import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;

/**
 * A product as a store chain lists it online: its name, barcode and the shelf price of one
 * package. packageQuantity is in packageUnit (e.g. 4000 g).
 */
public record StoreListing(
        String name,
        String brand,
        String gtin,
        BigDecimal price,
        String currency,
        BigDecimal packageQuantity,
        Unit packageUnit
) {}
