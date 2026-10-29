package com.lsoftware.inventoco.entity.catalog.model;

import com.lsoftware.inventoco.entity.unit.model.Dimension;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * A product shared by every user. Items point to it (by barcode or by choosing it), and that is
 * how the public prices one user records reach the others.
 */
public record CatalogProduct(
        UUID id,
        String name,
        String normalizedName,
        String brand,
        String gtin,
        Dimension dimension,
        Instant createdAt
) {

    public static CatalogProduct create(String name, String brand, String gtin, Dimension dimension, Instant now) {
        return new CatalogProduct(null, name.strip(), normalize(name), brand, gtin, dimension, now);
    }

    /** Lower case, no accents, single spaces: "Café  Molido" and "cafe molido" are the same product. */
    public static String normalize(String text) {
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
    }

    /** Digits only, 8 to 14 of them (EAN-8, UPC-A, EAN-13, GTIN-14); anything else is no barcode. */
    public static String gtin(String barcode) {
        if (barcode == null) return null;
        String digits = barcode.replaceAll("\\D", "");
        return digits.length() >= 8 && digits.length() <= 14 ? digits : null;
    }
}
