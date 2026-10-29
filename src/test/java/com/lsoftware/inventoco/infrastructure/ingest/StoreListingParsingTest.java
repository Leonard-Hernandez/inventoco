package com.lsoftware.inventoco.infrastructure.ingest;

import com.lsoftware.inventoco.entity.unit.model.Unit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Saved shapes of the stores' responses (trimmed), never the live sites. */
class StoreListingParsingTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void aVtexProductBecomesAListingWithBarcodePriceAndSize() {
        JsonNode p = json.readTree("""
                {"productName":"Arroz Diana 500 G","brand":"DIANA",
                 "items":[{"ean":"7702511000014","sellers":[{"commertialOffer":{"Price":1990.0,"AvailableQuantity":99999}}]}]}""");

        var l = VtexCatalogSource.toListing(p, "COP").orElseThrow();

        assertThat(l.name()).isEqualTo("Arroz Diana 500 G");
        assertThat(l.gtin()).isEqualTo("7702511000014");
        assertThat(l.price()).isEqualByComparingTo("1990");
        assertThat(l.packageQuantity()).isEqualByComparingTo("500");
        assertThat(l.packageUnit()).isEqualTo(Unit.G);
    }

    @Test
    void vtexProductsOutOfStockOrWithoutSizeAreSkipped() {
        JsonNode soldOut = json.readTree("""
                {"productName":"Arroz Diana 500 G","items":[{"ean":"1","sellers":[{"commertialOffer":{"Price":1990,"AvailableQuantity":0}}]}]}""");
        JsonNode noSize = json.readTree("""
                {"productName":"Bon Bon Bum Fresa","items":[{"ean":"1","sellers":[{"commertialOffer":{"Price":6990,"AvailableQuantity":5}}]}]}""");

        assertThat(VtexCatalogSource.toListing(soldOut, "COP")).isEmpty();
        assertThat(VtexCatalogSource.toListing(noSize, "COP")).isEmpty();
    }

    @Test
    void agroFruverPricesPerPoundHalfPoundOrTheSizeInTheName() {
        var lb = AgroFruverCatalogSource.toListing(json.readTree("""
                {"nombre":"Papa capira","precio":1900,"unidad_medida":"LIBRA","cantidad_stock":40}""")).orElseThrow();
        var half = AgroFruverCatalogSource.toListing(json.readTree("""
                {"nombre":"Arveja","precio":4300,"unidad_medida":"MEDIA LIBRA","cantidad_stock":3}""")).orElseThrow();
        var oil = AgroFruverCatalogSource.toListing(json.readTree("""
                {"nombre":"Aceite Oleocali 900ml","precio":11400,"unidad_medida":"UNIDAD","cantidad_stock":23}""")).orElseThrow();
        var avocado = AgroFruverCatalogSource.toListing(json.readTree("""
                {"nombre":"Aguacate Papelillo","precio":5000,"unidad_medida":"UNIDAD","cantidad_stock":8}""")).orElseThrow();

        assertThat(lb.packageUnit()).isEqualTo(Unit.LB);
        assertThat(lb.packageQuantity()).isEqualByComparingTo("1");
        assertThat(half.packageQuantity()).isEqualByComparingTo("0.5");
        assertThat(oil.packageUnit()).isEqualTo(Unit.ML);
        assertThat(oil.packageQuantity()).isEqualByComparingTo("900");
        assertThat(avocado.packageUnit()).isEqualTo(Unit.UNIT);
        assertThat(lb.gtin()).isNull();
        assertThat(AgroFruverCatalogSource.toListing(json.readTree("""
                {"nombre":"Mora","precio":4300,"unidad_medida":"LIBRA","cantidad_stock":0}"""))).isEmpty();
    }
}
