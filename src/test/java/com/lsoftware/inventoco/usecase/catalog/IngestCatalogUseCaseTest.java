package com.lsoftware.inventoco.usecase.catalog;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogGateway;
import com.lsoftware.inventoco.entity.catalog.gateway.CatalogSource;
import com.lsoftware.inventoco.entity.catalog.model.CatalogProduct;
import com.lsoftware.inventoco.entity.catalog.model.StoreListing;
import com.lsoftware.inventoco.entity.price.gateway.PriceGateway;
import com.lsoftware.inventoco.entity.price.model.PriceObservation;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.supplier.gateway.SupplierGateway;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lsoftware.inventoco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class IngestCatalogUseCaseTest {

    private final CatalogGateway catalog = mock(CatalogGateway.class);
    private final SupplierGateway suppliers = mock(SupplierGateway.class);
    private final PriceGateway prices = mock(PriceGateway.class);
    private final IngestCatalogUseCase useCase = new IngestCatalogUseCase(catalog, suppliers, prices, CLOCK);
    private final Supplier d1 = supplier(null, "D1");
    private final CatalogSource source = mock(CatalogSource.class);

    @BeforeEach
    void setUp() {
        when(source.chain()).thenReturn("D1");
        when(source.country()).thenReturn("CO");
        when(suppliers.findSystem("CO", "D1")).thenReturn(Optional.of(d1));
        when(catalog.save(any())).thenAnswer(i -> withId(i.getArgument(0)));
        when(prices.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void aListingBecomesACatalogProductAndAPublicPriceOfTheChainOwnedByNoUser() {
        when(source.search("/despensa", 100)).thenReturn(List.of(rice("1990")));

        var result = useCase.run(source, List.of("/despensa"), 100);

        assertThat(result).isEqualTo(new IngestCatalogUseCase.Result(1, 1, 1, 0));
        var price = ArgumentCaptor.forClass(PriceObservation.class);
        verify(prices).save(price.capture());
        assertThat(price.getValue().ownerId()).isNull();
        assertThat(price.getValue().itemId()).isNull();
        assertThat(price.getValue().supplierId()).isEqualTo(d1.id());
        assertThat(price.getValue().visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(price.getValue().unitPrice()).isEqualByComparingTo("3.98");
        assertThat(price.getValue().country()).isEqualTo("CO");
    }

    @Test
    void anUnchangedPriceSeenRecentlyIsNotStoredAgainButANewOneIs() {
        CatalogProduct product = withId(CatalogProduct.create("Arroz Diana 500 G", "DIANA", "7702511000014", Dimension.MASS, NOW));
        when(catalog.findByGtin("7702511000014")).thenReturn(Optional.of(product));
        when(prices.latestAt(d1.id(), product.id())).thenReturn(Optional.of(new PriceObservation(UUID.randomUUID(), null,
                null, d1.id(), product.id(), new BigDecimal("1990"), "COP", new BigDecimal("500"), Unit.G,
                new BigDecimal("3.98"), "CO", Visibility.PUBLIC, NOW.minusSeconds(3600), null, NOW)));
        when(source.search(any(), anyInt())).thenReturn(List.of(rice("1990"), rice("2090")));

        var result = useCase.run(source, List.of("arroz"), 50);

        assertThat(result).isEqualTo(new IngestCatalogUseCase.Result(2, 0, 1, 0));
        verify(prices, times(1)).save(any());
        verify(catalog, never()).save(any());
    }

    @Test
    void withoutBarcodeTheProductMatchesByNameAndAKindMismatchIsSkipped() {
        CatalogProduct banana = withId(CatalogProduct.create("Banano", null, null, Dimension.COUNT, NOW));
        when(catalog.findByNormalizedName("banano")).thenReturn(Optional.of(banana));
        when(source.search(any(), anyInt())).thenReturn(List.of(
                new StoreListing("Banano", null, null, new BigDecimal("1400"), "COP", BigDecimal.ONE, Unit.LB)));

        var result = useCase.run(source, List.of("*"), 50);

        assertThat(result).isEqualTo(new IngestCatalogUseCase.Result(1, 0, 0, 1));
        verify(prices, never()).save(any());
    }

    @Test
    void aBarcodeProductWhoseListingChangedDimensionKeepsItsDimensionAndTheConflictIsCounted() {
        CatalogProduct cloth = withId(CatalogProduct.create("Tela Lino 1,5 m", null, "7701234567890", Dimension.LENGTH, NOW));
        when(catalog.findByGtin("7701234567890")).thenReturn(Optional.of(cloth));
        when(source.search(any(), anyInt())).thenReturn(List.of(new StoreListing("Tela Lino x 10 Und", null,
                "7701234567890", new BigDecimal("50000"), "COP", BigDecimal.TEN, Unit.UNIT), rice("1990")));

        var result = useCase.run(source, List.of("/hogar"), 50);

        assertThat(result).isEqualTo(new IngestCatalogUseCase.Result(2, 1, 1, 1));
        verify(prices, never()).save(argThat(p -> cloth.id().equals(p.catalogProductId())));
        verify(catalog, never()).save(argThat(p -> "7701234567890".equals(p.gtin())));
    }

    @Test
    void aFailingSearchDoesNotStopTheOtherTerms() {
        when(source.search("/bebidas", 10)).thenThrow(new IllegalStateException("timeout"));
        when(source.search("/despensa", 10)).thenReturn(List.of(rice("1990")));

        assertThat(useCase.run(source, List.of("/bebidas", "/despensa"), 10).prices()).isEqualTo(1);
    }

    private static StoreListing rice(String price) {
        return new StoreListing("Arroz Diana 500 G", "DIANA", "7702511000014", new BigDecimal(price), "COP",
                new BigDecimal("500"), Unit.G);
    }

    private static CatalogProduct withId(CatalogProduct p) {
        return p.id() != null ? p : new CatalogProduct(UUID.nameUUIDFromBytes(p.normalizedName().getBytes()), p.name(),
                p.normalizedName(), p.brand(), p.gtin(), p.dimension(), p.createdAt());
    }
}
