package com.lsoftware.inventoco.entity.catalog;

import com.lsoftware.inventoco.entity.catalog.model.PackageSize;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PackageSizeTest {

    @Test
    void sizesAreReadFromStoreProductNames() {
        assertThat(PackageSize.parse("Arroz Diana 500 G")).contains(size("500", Unit.G));
        assertThat(PackageSize.parse("Arroz Estándar Albar 500 Grs")).contains(size("500", Unit.G));
        assertThat(PackageSize.parse("Aceite Vegetal Imatá 3.000 Ml")).contains(size("3000", Unit.ML));
        assertThat(PackageSize.parse("Soda Izots 1.7 Lts")).contains(size("1.7", Unit.L));
        assertThat(PackageSize.parse("Pastillas Chao Cereza 20,4 G")).contains(size("20.4", Unit.G));
        assertThat(PackageSize.parse("Harina de Maiz Blanco Quicksy X 1 Kg")).contains(size("1", Unit.KG));
        assertThat(PackageSize.parse("Aceite Oleocali 900ml")).contains(size("900", Unit.ML));
    }

    @Test
    void lengthsAndAreasAreReadToo() {
        assertThat(PackageSize.parse("Tela Lino Natural 1,5 m")).contains(size("1.5", Unit.M));
        assertThat(PackageSize.parse("Cable Duplex 10 Mts")).contains(size("10", Unit.M));
        assertThat(PackageSize.parse("Cinta de Enmascarar 24 mm x 40 m")).contains(size("40", Unit.M));
        assertThat(PackageSize.parse("Vinilo Adhesivo 2 m²")).contains(size("2", Unit.M2));
        assertThat(PackageSize.parse("Piso Laminado Caja 2.13 M2")).contains(size("2.13", Unit.M2));
    }

    @Test
    void weightOrVolumeWinsOverACountAndTheLastOneWritten() {
        assertThat(PackageSize.parse("Arepa de Chócolo 5 Und Masmaí 400 Grs")).contains(size("400", Unit.G));
        assertThat(PackageSize.parse("Spaguetti Doria Pague 250 G Lleve 275 G")).contains(size("275", Unit.G));
        assertThat(PackageSize.parse("Huevo Tipo Aa 30 Und")).contains(size("30", Unit.UNIT));
    }

    @Test
    void aCountWinsOverTheLengthOrAreaOfEachOne() {
        assertThat(PackageSize.parse("Platos Desechables 23 cm x 20 Und")).contains(size("20", Unit.UNIT));
        assertThat(PackageSize.parse("Bolsa Basura 50 x 60 cm 10 Und")).contains(size("10", Unit.UNIT));
        assertThat(PackageSize.parse("Piso Cerámico 60 x 60 cm Caja 1,44 m2")).contains(size("1.44", Unit.M2));
    }

    @Test
    void theLongestLengthIsTheRollNotItsWidth() {
        assertThat(PackageSize.parse("Papel Aluminio 7 m x 30 cm")).contains(size("7", Unit.M));
        assertThat(PackageSize.parse("Cinta 40 m x 24 mm")).contains(size("40", Unit.M));
        // a width is not a package size: fabric sold by the meter has none
        assertThat(PackageSize.parse("Tela antifluido ancho 1,5 m")).isEmpty();
        assertThat(PackageSize.parse("Tela antifluido ancho de 1,5 m rollo 50 m")).contains(size("50", Unit.M));
    }

    @Test
    void areaSpellingsAreNotMistakenForLengths() {
        assertThat(PackageSize.parse("Vinilo 1 metro cuadrado")).contains(size("1", Unit.M2));
        assertThat(PackageSize.parse("Vinilo 3 metros cuadrados")).contains(size("3", Unit.M2));
        assertThat(PackageSize.parse("Piso Cerámico 1,5 Mt2")).contains(size("1.5", Unit.M2));
        assertThat(PackageSize.parse("Piso Cerámico 2 mts2")).contains(size("2", Unit.M2));
    }

    @Test
    void cubicMetersAndSizeLettersAreNotLengths() {
        // cubic meters is not a supported unit
        assertThat(PackageSize.parse("Arena 1 m³")).isEmpty();
        assertThat(PackageSize.parse("Pañal Etapa 3 M x 30")).contains(size("30", Unit.UNIT));
        assertThat(PackageSize.parse("Huevos Rojos x 30")).contains(size("30", Unit.UNIT));
    }

    @Test
    void namesWithoutASizeHaveNone() {
        assertThat(PackageSize.parse("Aguacate Hass Unidad")).isEmpty();
        assertThat(PackageSize.parse("Bon Bon Bum Fresa")).isEmpty();
    }

    private static PackageSize size(String q, Unit u) {
        return new PackageSize(new java.math.BigDecimal(q), u);
    }
}
