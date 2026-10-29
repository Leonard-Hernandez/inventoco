package com.lsoftware.inventoco.usecase.item;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.item.exception.InsufficientStockException;
import com.lsoftware.inventoco.entity.item.exception.ItemNotFoundException;
import com.lsoftware.inventoco.entity.item.gateway.ItemGateway;
import com.lsoftware.inventoco.entity.item.gateway.MovementGateway;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.entity.item.model.StockMovement;
import com.lsoftware.inventoco.usecase.catalog.LinkCatalogProductUseCase;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase.MovementCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.lsoftware.inventoco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecordMovementUseCaseTest {

    private final ItemGateway items = mock(ItemGateway.class);
    private final MovementGateway movements = mock(MovementGateway.class);
    private final ManageItemUseCase itemQueries = new ManageItemUseCase(items, mock(LinkCatalogProductUseCase.class), CLOCK);
    private final RecordMovementUseCase useCase = new RecordMovementUseCase(items, movements, itemQueries, CLOCK);
    private final Item rice = rice(ANA, "2", "1", null);

    @BeforeEach
    void setUp() {
        when(items.findOwned(ANA.id(), rice.id())).thenReturn(Optional.of(rice));
        when(items.addStock(any(), any(), any())).thenReturn(true);
        when(movements.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void consumptionInAnotherUnitOfTheSameKindRemovesStockInGrams() {
        StockMovement m = useCase.execute(ANA.id(), rice.id(),
                new MovementCommand(MovementType.CONSUMPTION, new BigDecimal("500"), "g", null, " cena "));

        verify(items).addStock(eq(ANA.id()), eq(rice.id()), argThat(d -> d.compareTo(new BigDecimal("-500")) == 0));
        assertThat(m.quantity()).isEqualByComparingTo("-500");
        assertThat(m.occurredAt()).isEqualTo(NOW);
        assertThat(m.note()).isEqualTo("cena");
    }

    @Test
    void aPurchaseWithoutUnitUsesTheItemsUnit() {
        StockMovement m = useCase.execute(ANA.id(), rice.id(),
                new MovementCommand(MovementType.PURCHASE, new BigDecimal("1.5"), null, NOW.minusSeconds(3600), null));

        assertThat(m.quantity()).isEqualByComparingTo("1500");
        assertThat(m.occurredAt()).isEqualTo(NOW.minusSeconds(3600));
    }

    @Test
    void aCountStoresTheDifferenceWithTheStockReadUnderTheRowLock() {
        // Another device moved stock after the item was read: the locked row has 2.3 kg, not 2
        when(items.lockOwned(ANA.id(), rice.id())).thenReturn(Optional.of(new Item(rice.id(), ANA.id(), rice.name(),
                rice.category(), rice.dimension(), rice.displayUnit(), rice.minStock(), new BigDecimal("2300"), null, null,
                false, NOW, NOW)));

        StockMovement m = useCase.execute(ANA.id(), rice.id(),
                new MovementCommand(MovementType.ADJUSTMENT, new BigDecimal("1.2"), "kg", null, null));

        assertThat(m.quantity()).isEqualByComparingTo("-1100");
        verify(items).lockOwned(ANA.id(), rice.id());
    }

    @Test
    void theStockNeverGoesBelowZero() {
        when(items.addStock(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(ANA.id(), rice.id(),
                new MovementCommand(MovementType.CONSUMPTION, new BigDecimal("3"), "kg", null, null)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Only 2 kg in stock");
        verify(movements, never()).save(any());
    }

    @Test
    void unitsOfAnotherKindZeroAndFutureMovementsAreRejected() {
        assertThatThrownBy(() -> useCase.execute(ANA.id(), rice.id(),
                new MovementCommand(MovementType.CONSUMPTION, BigDecimal.ONE, "l", null, null)))
                .hasMessageContaining("measures volume");
        assertThatThrownBy(() -> useCase.execute(ANA.id(), rice.id(),
                new MovementCommand(MovementType.CONSUMPTION, BigDecimal.ZERO, null, null, null)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> useCase.execute(ANA.id(), rice.id(),
                new MovementCommand(MovementType.PURCHASE, BigDecimal.ONE, null, NOW.plusSeconds(3600), null)))
                .hasMessageContaining("future");
        verify(items, never()).addStock(any(), any(), any());
    }

    @Test
    void anotherUsersItemIsNotFound() {
        when(items.findOwned(LUIS.id(), rice.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(LUIS.id(), rice.id(),
                new MovementCommand(MovementType.CONSUMPTION, BigDecimal.ONE, null, null, null)))
                .isInstanceOf(ItemNotFoundException.class);
        assertThatThrownBy(() -> itemQueries.get(LUIS.id(), rice.id())).isInstanceOf(ItemNotFoundException.class);
    }
}
