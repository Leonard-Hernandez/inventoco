package com.lsoftware.inventoco.entity.catalog.gateway;

import com.lsoftware.inventoco.entity.catalog.model.StoreListing;

import java.util.List;

/** A store chain's online catalog, searched by text. */
public interface CatalogSource {
    /** Name of the chain as users know it ("Éxito"); it becomes a system supplier. */
    String chain();
    String country();
    /** Listings found for a search term; ones without barcode, price or package size are left out. */
    List<StoreListing> search(String term, int limit);
}
