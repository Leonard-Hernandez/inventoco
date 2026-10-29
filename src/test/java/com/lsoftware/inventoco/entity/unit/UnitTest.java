package com.lsoftware.inventoco.entity.unit;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitTest {

    @Test
    void quantitiesAreStoredInTheBaseUnitOfTheirDimension() {
        assertThat(Unit.KG.toBase(new BigDecimal("1.5"))).isEqualByComparingTo("1500");
        assertThat(Unit.L.toBase(new BigDecimal("2"))).isEqualByComparingTo("2000");
        assertThat(Unit.DOZEN.toBase(new BigDecimal("2"))).isEqualByComparingTo("24");
        assertThat(Unit.LB.toBase(BigDecimal.ONE)).isEqualByComparingTo("453.59237");
    }

    @Test
    void baseQuantitiesAreShownInAnyUnitOfTheSameDimensionWithoutScientificNotation() {
        assertThat(Unit.KG.fromBase(new BigDecimal("2500")).toString()).isEqualTo("2.5");
        assertThat(Unit.G.fromBase(new BigDecimal("1000.000000")).toString()).isEqualTo("1000");
        assertThat(Unit.GAL.fromBase(Unit.GAL.toBase(BigDecimal.ONE))).isEqualByComparingTo("1");
    }

    @Test
    void unitsAreParsedBySymbolNameOrAliasInAnyCase() {
        assertThat(Unit.parse("Kg")).isEqualTo(Unit.KG);
        assertThat(Unit.parse("litro")).isEqualTo(Unit.L);
        assertThat(Unit.parse("fl oz")).isEqualTo(Unit.FL_OZ);
        assertThat(Unit.parse("und")).isEqualTo(Unit.UNIT);
        assertThatThrownBy(() -> Unit.parse("bushel")).hasMessageContaining("Unknown unit");
    }

    @Test
    void aUnitOfAnotherDimensionIsRejected() {
        assertThatThrownBy(() -> Unit.parse("ml", Dimension.MASS))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ml measures volume, this item is measured in mass");
    }
}
