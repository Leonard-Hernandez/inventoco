package com.lsoftware.inventoco.infrastructure.ingest;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogSource;
import com.lsoftware.inventoco.entity.catalog.model.PackageSize;
import com.lsoftware.inventoco.entity.catalog.model.StoreListing;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A store chain on VTEX (D1, Éxito, Carulla): its public catalog API. A term starting with "/" is a
 * category path ("/despensa"), anything else a text search. Pages of 50, one request at a time
 * with a pause in between; the package size comes from the product name ("Arroz Diana 500 G").
 */
public class VtexCatalogSource implements CatalogSource {

    static final int PAGE = 50;

    private final String chain;
    private final String country;
    private final String currency;
    private final RestClient http;
    private final long pauseMillis;

    public VtexCatalogSource(String chain, String country, String currency, String baseUrl, RestClient.Builder builder,
                             long pauseMillis) {
        this.chain = chain;
        this.country = country;
        this.currency = currency;
        this.http = builder.baseUrl(baseUrl).defaultHeader("User-Agent", "Inventoco catalog (price comparison)").build();
        this.pauseMillis = pauseMillis;
    }

    @Override
    public String chain() {
        return chain;
    }

    @Override
    public String country() {
        return country;
    }

    @Override
    public List<StoreListing> search(String term, int limit) {
        List<StoreListing> out = new ArrayList<>();
        for (int from = 0; from < limit; from += PAGE) {
            int to = Math.min(from + PAGE, limit) - 1;
            String path = term.startsWith("/")
                    ? "/api/catalog_system/pub/products/search" + term + "?_from=" + from + "&_to=" + to
                    : "/api/catalog_system/pub/products/search?ft=" + URLEncoder.encode(term, StandardCharsets.UTF_8)
                            + "&_from=" + from + "&_to=" + to;
            JsonNode page = http.get().uri(path).retrieve().body(JsonNode.class);
            if (page == null || !page.isArray() || page.isEmpty()) break;
            for (JsonNode p : page) toListing(p, currency).ifPresent(out::add);
            pause();
            if (page.size() < to - from + 1) break;
        }
        return out;
    }

    static java.util.Optional<StoreListing> toListing(JsonNode p, String currency) {
        JsonNode item = p.path("items").path(0);
        JsonNode offer = item.path("sellers").path(0).path("commertialOffer");
        String name = p.path("productName").asString(null);
        BigDecimal price = offer.path("Price").isNumber() ? offer.path("Price").decimalValue() : null;
        if (name == null || price == null || price.signum() <= 0 || offer.path("AvailableQuantity").asInt(0) <= 0) {
            return java.util.Optional.empty();
        }
        return PackageSize.parse(name).map(size -> new StoreListing(name, p.path("brand").asString(null),
                item.path("ean").asString(null), price, currency, size.quantity(), size.unit()));
    }

    private void pause() {
        try {
            Thread.sleep(pauseMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
