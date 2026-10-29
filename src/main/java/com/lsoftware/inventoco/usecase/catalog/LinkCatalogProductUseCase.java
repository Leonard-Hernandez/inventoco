package com.lsoftware.inventoco.usecase.catalog;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogGateway;
import com.lsoftware.inventoco.entity.catalog.model.CatalogProduct;
import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.exception.NotFoundException;
import com.lsoftware.inventoco.entity.unit.model.Dimension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Which shared catalog product an item is. A chosen product wins; otherwise the barcode finds
 * (or creates) it. Items without either stay private to their owner's price history.
 */
@Component
@RequiredArgsConstructor
public class LinkCatalogProductUseCase {

    private final CatalogGateway catalog;
    private final Clock clock;

    public UUID resolve(UUID chosenId, String barcode, String itemName, Dimension dimension) {
        if (chosenId != null) {
            CatalogProduct product = catalog.findById(chosenId)
                    .orElseThrow(() -> new NotFoundException("CATALOG_PRODUCT_NOT_FOUND", "Catalog product not found"));
            return checked(product, dimension).id();
        }
        String gtin = CatalogProduct.gtin(barcode);
        if (gtin == null) return null;
        return checked(catalog.findByGtin(gtin)
                .orElseGet(() -> catalog.save(CatalogProduct.create(itemName, null, gtin, dimension, clock.instant()))),
                dimension).id();
    }

    private static CatalogProduct checked(CatalogProduct product, Dimension dimension) {
        if (product.dimension() != dimension) {
            throw new DomainException("INCOMPATIBLE_UNIT", product.name() + " is measured by "
                    + product.dimension().name().toLowerCase() + ", not " + dimension.name().toLowerCase());
        }
        return product;
    }
}
