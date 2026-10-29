package com.lsoftware.inventoco.usecase.price;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogGateway;
import com.lsoftware.inventoco.entity.exception.NotFoundException;
import com.lsoftware.inventoco.entity.item.gateway.ItemGateway;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.price.gateway.PriceGateway;
import com.lsoftware.inventoco.entity.price.model.PriceObservation;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.supplier.gateway.SupplierGateway;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.usecase.catalog.LinkCatalogProductUseCase;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import com.lsoftware.inventoco.usecase.price.PriceUseCase.PriceCommand;
import com.lsoftware.inventoco.usecase.supplier.ManageSupplierUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lsoftware.inventoco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceUseCaseTest {

    private static final UUID RICE_PRODUCT = UUID.randomUUID();

    private final PriceGateway prices = mock(PriceGateway.class);
    private final SupplierGateway supplierGateway = mock(SupplierGateway.class);
    private final CatalogGateway catalog = mock(CatalogGateway.class);
    private final ItemGateway itemGateway = mock(ItemGateway.class);
    private final ManageItemUseCase items = new ManageItemUseCase(itemGateway, mock(LinkCatalogProductUseCase.class), CLOCK);
    private final ManageSupplierUseCase suppliers = new ManageSupplierUseCase(supplierGateway, CLOCK);
    private final PriceUseCase useCase = new PriceUseCase(prices, supplierGateway, catalog, items, suppliers, CLOCK);

    private final Item rice = rice(ANA, "2", "1", RICE_PRODUCT);
    private final Supplier tienda = supplier(ANA, "Tienda Don Pepe");
    private final Supplier exito = supplier(null, "Éxito");
    private final Supplier luisShop = supplier(LUIS, "Mercado de Luis");

    @BeforeEach
    void setUp() {
        when(itemGateway.findOwned(ANA.id(), rice.id())).thenReturn(Optional.of(rice));
        for (Supplier s : List.of(tienda, exito, luisShop)) when(supplierGateway.findById(s.id())).thenReturn(Optional.of(s));
        when(supplierGateway.findAllById(anyCollection())).thenReturn(List.of(tienda, exito, luisShop));
        when(prices.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void aPriceIsNormalizedPerBaseUnitInTheUsersCurrency() {
        PriceObservation p = useCase.record(ANA, rice.id(), new PriceCommand(tienda.id(), new BigDecimal("12900"),
                new BigDecimal("2500"), "g", null, null));

        assertThat(p.unitPrice()).isEqualByComparingTo("5.16");
        assertThat(p.packageQuantity()).isEqualByComparingTo("2500");
        assertThat(p.packageUnit()).isEqualTo(Unit.G);
        assertThat(p.currency()).isEqualTo("COP");
        assertThat(p.country()).isEqualTo("CO");
        assertThat(p.visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(p.catalogProductId()).isEqualTo(RICE_PRODUCT);
        assertThat(p.observedAt()).isEqualTo(NOW);
    }

    @Test
    void visibilityDefaultsToTheUsersSettingButStaysPrivateWithoutACatalogProduct() {
        var sharing = ANA.withSettings("CO", "COP", BOGOTA, true);
        PriceCommand c = new PriceCommand(exito.id(), new BigDecimal("5000"), BigDecimal.ONE, "kg", null, null);

        assertThat(useCase.record(sharing, rice, c, null).visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(useCase.record(sharing, rice(ANA, "1", "0", null), c, null).visibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void onlyOwnOrStoreChainSuppliersCanBeUsed() {
        assertThatThrownBy(() -> useCase.record(ANA, rice.id(), new PriceCommand(luisShop.id(), BigDecimal.TEN,
                BigDecimal.ONE, null, null, null))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void theBestOfferIsTheCheapestOwnOrPublicPriceAndTheUsualOneTheLatestPaid() {
        PriceObservation mineOld = price(ANA, tienda, "4.0", NOW.minus(Duration.ofDays(20)), Visibility.PRIVATE);
        PriceObservation mineLatest = price(ANA, tienda, "5.0", NOW.minus(Duration.ofDays(2)), Visibility.PRIVATE);
        PriceObservation luis = price(LUIS, luisShop, "3.5", NOW.minus(Duration.ofDays(5)), Visibility.PUBLIC);
        when(prices.listByOwnerSince(eq(ANA.id()), any())).thenReturn(List.of(mineOld, mineLatest));
        when(prices.listPublic(eq(List.of(RICE_PRODUCT)), eq("CO"), eq("COP"), any())).thenReturn(List.of(luis));

        var offers = useCase.offers(ANA, List.of(rice)).get(rice.id());

        assertThat(offers.best().unitPrice()).isEqualByComparingTo("3.5");
        assertThat(offers.best().supplierName()).isEqualTo("Mercado de Luis");
        assertThat(offers.best().supplierId()).as("another user's supplier cannot be picked").isNull();
        assertThat(offers.best().source()).isEqualTo(Offer.Source.PUBLIC);
        assertThat(offers.usual().unitPrice()).isEqualByComparingTo("5.0");
        assertThat(offers.usual().supplierId()).isEqualTo(tienda.id());
    }

    @Test
    void publicPricesNeverSayWhoRecordedThem() {
        when(catalog.findById(RICE_PRODUCT)).thenReturn(Optional.of(
                new com.lsoftware.inventoco.entity.catalog.model.CatalogProduct(RICE_PRODUCT, "Arroz", "arroz", null,
                        null, Unit.KG.dimension(), NOW)));
        when(prices.listPublic(eq(List.of(RICE_PRODUCT)), eq("CO"), eq("COP"), any()))
                .thenReturn(List.of(price(LUIS, luisShop, "3.5", NOW, Visibility.PUBLIC)));

        List<PublicPrice> list = useCase.publicForProduct(ANA, RICE_PRODUCT);

        assertThat(list).singleElement().satisfies(p -> {
            assertThat(p.supplierName()).isEqualTo("Mercado de Luis");
            assertThat(p.packageQuantity()).isEqualByComparingTo("1");
            assertThat(p.packageUnit()).isEqualTo("kg");
        });
        assertThat(Arrays.stream(PublicPrice.class.getRecordComponents()).map(c -> c.getName()))
                .noneMatch(n -> n.toLowerCase().contains("owner") || n.toLowerCase().contains("user")
                        || n.toLowerCase().contains("email") || n.equals("itemId") || n.equals("purchaseId"));
    }

    private PriceObservation price(com.lsoftware.inventoco.entity.profile.model.UserProfile owner, Supplier s,
                                   String perGram, Instant at, Visibility v) {
        BigDecimal unit = new BigDecimal(perGram);
        return new PriceObservation(UUID.randomUUID(), owner.id(), owner == ANA ? rice.id() : UUID.randomUUID(), s.id(),
                RICE_PRODUCT, unit.multiply(new BigDecimal("1000")), "COP", new BigDecimal("1000"), Unit.KG, unit, "CO",
                v, at, null, at);
    }
}
