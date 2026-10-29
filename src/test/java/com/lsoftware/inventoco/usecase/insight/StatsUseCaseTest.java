package com.lsoftware.inventoco.usecase.insight;

import com.lsoftware.inventoco.entity.price.gateway.PriceGateway;
import com.lsoftware.inventoco.entity.price.model.PriceObservation;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.supplier.gateway.SupplierGateway;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lsoftware.inventoco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsUseCaseTest {

    private final PriceGateway prices = mock(PriceGateway.class);
    private final SupplierGateway suppliers = mock(SupplierGateway.class);
    private final ShoppingListUseCase shopping = mock(ShoppingListUseCase.class);
    private final StatsUseCase useCase = new StatsUseCase(prices, suppliers, shopping, CLOCK);

    private final UUID rice = UUID.randomUUID();
    private final UUID oil = UUID.randomUUID();
    private final Supplier cheap = supplier(ANA, "Barato");
    private final Supplier pricey = supplier(ANA, "Caro");

    @Test
    void realizedSavingsCompareWhatWasPaidWithTheItemsAveragePrice() {
        UUID purchase = UUID.randomUUID();
        when(prices.listByOwnerSince(eq(ANA.id()), any())).thenReturn(List.of(
                price(rice, pricey, "6", 200, null),          // seen, not bought: only in the average
                price(rice, cheap, "4", 10, purchase),        // bought 1 kg at 4/g, average 5/g
                price(rice, cheap, "5", 300, purchase),       // bought before the period
                price(oil, cheap, "10", 5, purchase),
                usd(price(oil, cheap, "1", 5, purchase))));  // other currency: ignored
        when(shopping.draft(ANA)).thenReturn(Optional.empty());

        var s = useCase.savings(ANA, 90);

        assertThat(s.currency()).isEqualTo("COP");
        assertThat(s.realized()).isEqualByComparingTo("1000.00");
        assertThat(s.spent()).isEqualByComparingTo("14000.00");
        assertThat(s.purchases()).isEqualTo(2);
        assertThat(s.estimatedOnShoppingList()).isEqualByComparingTo("0");
    }

    @Test
    void suppliersAreRankedByHowTheirPricesCompareWithTheTypicalOne() {
        when(prices.listByOwnerSince(eq(ANA.id()), any())).thenReturn(List.of(
                price(rice, cheap, "4", 10, null), price(rice, pricey, "6", 9, null),
                price(oil, cheap, "9", 10, null), price(oil, pricey, "11", 9, null)));
        when(suppliers.findAllById(anyCollection())).thenReturn(List.of(cheap, pricey));

        var ranking = useCase.supplierRanking(ANA, 180);

        assertThat(ranking).extracting(StatsUseCase.SupplierRank::name).containsExactly("Barato", "Caro");
        assertThat(ranking.getFirst().priceIndex()).isEqualByComparingTo("0.850");
        assertThat(ranking.getFirst().itemsCheapest()).isEqualTo(2);
        assertThat(ranking.get(1).priceIndex()).isEqualByComparingTo("1.150");
    }

    private PriceObservation price(UUID item, Supplier s, String perGram, int daysAgo, UUID purchase) {
        BigDecimal unit = new BigDecimal(perGram);
        var at = NOW.minus(Duration.ofDays(daysAgo));
        return new PriceObservation(UUID.randomUUID(), ANA.id(), item, s.id(), null, unit.multiply(new BigDecimal("1000")),
                "COP", new BigDecimal("1000"), Unit.KG, unit, "CO", Visibility.PRIVATE, at, purchase, at);
    }

    private static PriceObservation usd(PriceObservation p) {
        return new PriceObservation(p.id(), p.ownerId(), p.itemId(), p.supplierId(), null, p.amount(), "USD",
                p.packageQuantity(), p.packageUnit(), p.unitPrice(), p.country(), p.visibility(), p.observedAt(),
                p.purchaseId(), p.createdAt());
    }
}
