package com.lsoftware.inventoco.usecase.shopping;

import com.lsoftware.inventoco.entity.exception.ConflictException;
import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.purchase.gateway.PurchaseGateway;
import com.lsoftware.inventoco.entity.purchase.model.LineReason;
import com.lsoftware.inventoco.entity.purchase.model.Purchase;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseLine;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseStatus;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.usecase.insight.DepletionForecast;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase.ItemForecast;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase;
import com.lsoftware.inventoco.usecase.price.Offer;
import com.lsoftware.inventoco.usecase.price.PriceUseCase;
import com.lsoftware.inventoco.usecase.price.PriceUseCase.ItemOffers;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase.BoughtLine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.lsoftware.inventoco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShoppingListUseCaseTest {

    private final PurchaseGateway purchases = mock(PurchaseGateway.class);
    private final ForecastUseCase forecasts = mock(ForecastUseCase.class);
    private final PriceUseCase prices = mock(PriceUseCase.class);
    private final ManageItemUseCase items = mock(ManageItemUseCase.class);
    private final RecordMovementUseCase movements = mock(RecordMovementUseCase.class);
    private final ShoppingListUseCase useCase = new ShoppingListUseCase(purchases, forecasts, prices, items, movements, CLOCK);

    private final Supplier tienda = supplier(ANA, "Tienda");
    private final Supplier d1 = supplier(null, "D1");

    @Test
    void lowAndRunningOutItemsAreListedWithQuantityCheapestSupplierAndSavings() {
        Item low = rice(ANA, "0.5", "1", null);                       // at minimum
        Item oil = new Item(UUID.randomUUID(), ANA.id(), "Aceite", null, com.lsoftware.inventoco.entity.unit.model.Dimension.VOLUME,
                com.lsoftware.inventoco.entity.unit.model.Unit.L, BigDecimal.ZERO, new BigDecimal("300"), null, null, false, NOW, NOW);
        Item fine = rice(ANA, "10", "1", null);
        when(forecasts.all(ANA, 7)).thenReturn(List.of(
                forecast(low, "100", true), forecast(oil, "100", true), forecast(fine, "10", false)));
        when(prices.offers(eq(ANA), anyCollection())).thenReturn(Map.of(
                low.id(), new ItemOffers(new Offer(d1.id(), "D1", new BigDecimal("4"), NOW, Offer.Source.OWN),
                        new Offer(tienda.id(), "Tienda", new BigDecimal("5"), NOW, Offer.Source.OWN))));
        when(purchases.findDraft(ANA.id())).thenReturn(Optional.of(draft(List.of())));
        when(purchases.save(any())).thenAnswer(i -> i.getArgument(0));

        Purchase list = useCase.generate(ANA, null);

        verify(purchases).delete(any());
        assertThat(list.status()).isEqualTo(PurchaseStatus.DRAFT);
        assertThat(list.lines()).hasSize(2);
        PurchaseLine rice = list.lines().getFirst();
        // back to 1 kg minimum + 7 days x 100 g - 500 g in stock = 1.2 kg
        assertThat(rice.suggestedQuantity()).isEqualByComparingTo("1200");
        assertThat(rice.reason()).isEqualTo(LineReason.LOW_STOCK);
        assertThat(rice.supplierId()).isEqualTo(d1.id());
        assertThat(ShoppingListUseCase.estimatedSavings(rice)).isEqualByComparingTo("1200");
        assertThat(ShoppingListUseCase.estimatedCost(rice)).isEqualByComparingTo("4800");
        PurchaseLine oilLine = list.lines().get(1);
        // 700 ml needed - 300 ml in stock = 0.4 l
        assertThat(oilLine.suggestedQuantity()).isEqualByComparingTo("400");
        assertThat(oilLine.reason()).isEqualTo(LineReason.RUNNING_OUT);
        assertThat(oilLine.supplierId()).isNull();
    }

    @Test
    void countableItemsAreSuggestedInWholeUnits() {
        Item eggs = new Item(UUID.randomUUID(), ANA.id(), "Huevos", null, com.lsoftware.inventoco.entity.unit.model.Dimension.COUNT,
                com.lsoftware.inventoco.entity.unit.model.Unit.UNIT, new BigDecimal("6"), new BigDecimal("4"), null, null,
                false, NOW, NOW);

        BigDecimal q = ShoppingListUseCase.suggestedQuantity(forecast(eggs, "1.3", true), 7);

        // 6 + 9.1 - 4 = 11.1 -> 12
        assertThat(q).isEqualByComparingTo("12");
    }

    @Test
    void completingTheListAddsTheStockAndRecordsThePricesPaid() {
        Item rice = rice(ANA, "0.5", "1", null);
        Purchase draft = draft(List.of());
        when(purchases.lockOwned(ANA.id(), draft.id())).thenReturn(Optional.of(draft));
        when(purchases.save(any())).thenAnswer(i -> i.getArgument(0));
        when(items.get(ANA.id(), rice.id())).thenReturn(rice);

        Purchase done = useCase.complete(ANA, draft.id(), List.of(
                new BoughtLine(rice.id(), new BigDecimal("2"), "kg", tienda.id(), new BigDecimal("9800"), Visibility.PUBLIC),
                new BoughtLine(UUID.randomUUID(), BigDecimal.ZERO, null, null, null, null)), null);

        assertThat(done.status()).isEqualTo(PurchaseStatus.COMPLETED);
        assertThat(done.completedAt()).isEqualTo(NOW);
        var movement = ArgumentCaptor.forClass(RecordMovementUseCase.MovementCommand.class);
        verify(movements).record(eq(ANA.id()), eq(rice), movement.capture(), eq(draft.id()));
        assertThat(movement.getValue().type()).isEqualTo(MovementType.PURCHASE);
        assertThat(movement.getValue().quantity()).isEqualByComparingTo("2");
        var price = ArgumentCaptor.forClass(PriceUseCase.PriceCommand.class);
        verify(prices).record(eq(ANA), eq(rice), price.capture(), eq(draft.id()));
        assertThat(price.getValue().amount()).isEqualByComparingTo("9800");
        assertThat(price.getValue().visibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test
    void aListIsCompletedOnceAndNeedsSomethingBought() {
        Purchase done = draft(List.of()).completed(NOW);
        when(purchases.lockOwned(ANA.id(), done.id())).thenReturn(Optional.of(done));
        Purchase open = draft(List.of());
        when(purchases.lockOwned(ANA.id(), open.id())).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> useCase.complete(ANA, done.id(), List.of(), null)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> useCase.complete(ANA, open.id(), List.of(
                new BoughtLine(UUID.randomUUID(), BigDecimal.ZERO, null, null, null, null)), null))
                .isInstanceOf(DomainException.class).hasMessageContaining("Nothing was bought");
    }

    private static ItemForecast forecast(Item item, String gramsPerDay, boolean soon) {
        return new ItemForecast(item, new DepletionForecast.Result(new BigDecimal(gramsPerDay), null, null,
                DepletionForecast.Confidence.HIGH, 20), soon);
    }

    private static Purchase draft(List<PurchaseLine> lines) {
        return new Purchase(UUID.randomUUID(), ANA.id(), PurchaseStatus.DRAFT, "COP", 7, NOW, null, lines);
    }
}
