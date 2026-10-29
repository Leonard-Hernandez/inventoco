package com.lsoftware.inventoco.infrastructure.ingest;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogSource;
import com.lsoftware.inventoco.entity.catalog.model.PackageSize;
import com.lsoftware.inventoco.entity.catalog.model.StoreListing;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Agro Fruver (fruit and vegetable kiosks in Cali): its live inventory is one public JSON list.
 * Fresh produce is priced per pound (LIBRA, MEDIA LIBRA) or kilo; packaged goods per unit with the
 * size in the name ("Aceite Oleocali 900ml"). It has no barcodes, so products match by name.
 * The whole list is read once per run; the search term is ignored.
 */
public class AgroFruverCatalogSource implements CatalogSource {

    private final RestClient http;
    private final String url;

    public AgroFruverCatalogSource(String url, RestClient.Builder builder) {
        this.http = builder.defaultHeader("User-Agent", "Inventoco catalog (price comparison)").build();
        this.url = url;
    }

    @Override
    public String chain() {
        return "Agro Fruver";
    }

    @Override
    public String country() {
        return "CO";
    }

    @Override
    public List<StoreListing> search(String term, int limit) {
        JsonNode body = http.get().uri(url).retrieve().body(JsonNode.class);
        List<StoreListing> out = new ArrayList<>();
        if (body == null) return out;
        for (JsonNode p : body.path("productos")) {
            toListing(p).ifPresent(out::add);
            if (out.size() >= limit) break;
        }
        return out;
    }

    static Optional<StoreListing> toListing(JsonNode p) {
        String name = p.path("nombre").asString(null);
        BigDecimal price = p.path("precio").isNumber() ? p.path("precio").decimalValue() : null;
        if (name == null || price == null || price.signum() <= 0 || p.path("cantidad_stock").asInt(0) <= 0) {
            return Optional.empty();
        }
        PackageSize size = switch (p.path("unidad_medida").asString("")) {
            case "LIBRA" -> new PackageSize(BigDecimal.ONE, Unit.LB);
            case "MEDIA LIBRA" -> new PackageSize(new BigDecimal("0.5"), Unit.LB);
            case "KILO" -> new PackageSize(BigDecimal.ONE, Unit.KG);
            // Units, trays, bags and nets: the size written in the name, or one of them
            default -> PackageSize.parse(name).orElse(new PackageSize(BigDecimal.ONE, Unit.UNIT));
        };
        return Optional.of(new StoreListing(name.strip(), null, null, price, "COP", size.quantity(), size.unit()));
    }
}
