package com.lsoftware.inventoco.usecase.shopping;

import com.lsoftware.inventoco.entity.exception.ConflictException;
import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.exception.NotFoundException;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.entity.purchase.gateway.PurchaseGateway;
import com.lsoftware.inventoco.entity.purchase.model.LineReason;
import com.lsoftware.inventoco.entity.purchase.model.Purchase;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseLine;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseStatus;
import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase.ItemForecast;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase.MovementCommand;
import com.lsoftware.inventoco.usecase.price.Offer;
import com.lsoftware.inventoco.usecase.price.PriceUseCase;
import com.lsoftware.inventoco.usecase.price.PriceUseCase.ItemOffers;
import com.lsoftware.inventoco.usecase.price.PriceUseCase.PriceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * "What's missing": a shopping list with every item at or below its minimum, plus the ones the
 * forecast says run out within the horizon. Each line suggests how much to buy (back to the
 * minimum plus what is consumed over the horizon) and where it is cheapest. Completing the list
 * turns what was actually bought into purchases (stock) and prices, in one transaction.
 */
@Component
@RequiredArgsConstructor
public class ShoppingListUseCase {

    public static final int DEFAULT_HORIZON_DAYS = 7;

    private final PurchaseGateway purchases;
    private final ForecastUseCase forecasts;
    private final PriceUseCase prices;
    private final ManageItemUseCase items;
    private final RecordMovementUseCase movements;
    private final Clock clock;

    /** Builds a new list, replacing the open one. */
    @Transactional
    public Purchase generate(UserProfile user, Integer horizonDays) {
        int horizon = horizonDays == null ? DEFAULT_HORIZON_DAYS : Math.clamp(horizonDays, 1, 90);
        List<ItemForecast> needed = forecasts.all(user, horizon).stream().filter(ItemForecast::buySoon).toList();
        Map<UUID, ItemOffers> offers = prices.offers(user, needed.stream().map(ItemForecast::item).toList());

        List<PurchaseLine> lines = new ArrayList<>();
        for (ItemForecast f : needed) {
            BigDecimal quantity = suggestedQuantity(f, horizon);
            if (quantity.signum() <= 0) continue;
            ItemOffers o = offers.get(f.item().id());
            Offer best = o == null ? null : o.best();
            Offer usual = o == null ? null : o.usual();
            lines.add(new PurchaseLine(null, f.item().id(), lines.size(),
                    f.item().lowStock() ? LineReason.LOW_STOCK : LineReason.RUNNING_OUT, quantity,
                    pickableSupplier(best, usual), best == null ? null : best.supplierName(),
                    best == null ? null : best.unitPrice(), usual == null ? null : usual.unitPrice()));
        }
        purchases.findDraft(user.id()).ifPresent(d -> purchases.delete(d.id()));
        return purchases.save(new Purchase(null, user.id(), PurchaseStatus.DRAFT, user.currency(), horizon,
                clock.instant(), null, lines));
    }

    public Optional<Purchase> draft(UserProfile user) {
        return purchases.findDraft(user.id());
    }

    public List<Purchase> history(UserProfile user, int limit) {
        return purchases.listCompleted(user.id(), Math.clamp(limit, 1, 100));
    }

    /**
     * What was bought. Lines can differ from the suggestion (other quantities, extra items).
     *
     * @param quantity  in unit (the item's display unit when null); 0 = not bought
     * @param amount    total paid for the line; with supplierId it is also recorded as a price
     */
    public record BoughtLine(UUID itemId, BigDecimal quantity, String unit, UUID supplierId, BigDecimal amount,
                             Visibility visibility) {}

    @Transactional
    public Purchase complete(UserProfile user, UUID purchaseId, List<BoughtLine> bought, Instant occurredAt) {
        Purchase purchase = purchases.lockOwned(user.id(), purchaseId)
                .orElseThrow(() -> new NotFoundException("PURCHASE_NOT_FOUND", "Shopping list not found"));
        if (purchase.status() != PurchaseStatus.DRAFT) {
            throw new ConflictException("PURCHASE_COMPLETED", "This shopping list was already completed");
        }
        recordLines(user, purchase.id(), bought, occurredAt);
        return purchases.save(purchase.completed(clock.instant()));
    }

    /** A purchase made without a shopping list (e.g. told to the assistant). */
    @Transactional
    public Purchase recordPurchase(UserProfile user, List<BoughtLine> bought, Instant occurredAt) {
        Instant now = clock.instant();
        Purchase purchase = purchases.save(new Purchase(null, user.id(), PurchaseStatus.COMPLETED, user.currency(),
                0, now, now, List.of()));
        recordLines(user, purchase.id(), bought, occurredAt);
        return purchase;
    }

    private void recordLines(UserProfile user, UUID purchaseId, List<BoughtLine> bought, Instant occurredAt) {
        if (bought == null || bought.stream().noneMatch(l -> l.quantity() != null && l.quantity().signum() > 0)) {
            throw new DomainException("EMPTY_PURCHASE", "Nothing was bought");
        }
        Instant at = occurredAt == null ? clock.instant() : occurredAt;
        var seen = new HashSet<UUID>();
        for (BoughtLine line : bought) {
            if (line.quantity() == null || line.quantity().signum() == 0) continue;
            if (!seen.add(line.itemId())) {
                throw new DomainException("DUPLICATE_ITEM", "Each item can appear only once");
            }
            Item item = items.get(user.id(), line.itemId());
            movements.record(user.id(), item,
                    new MovementCommand(MovementType.PURCHASE, line.quantity(), line.unit(), at, null), purchaseId);
            if (line.supplierId() != null && line.amount() != null) {
                prices.record(user, item, new PriceCommand(line.supplierId(), line.amount(), line.quantity(),
                        line.unit(), at, line.visibility()), purchaseId);
            }
        }
    }

    /** Estimated savings of a line: buying at the best price instead of the usual one. */
    public static BigDecimal estimatedSavings(PurchaseLine line) {
        if (line.bestUnitPrice() == null || line.usualUnitPrice() == null) return BigDecimal.ZERO;
        BigDecimal diff = line.usualUnitPrice().subtract(line.bestUnitPrice());
        if (diff.signum() <= 0) return BigDecimal.ZERO;
        return diff.multiply(line.suggestedQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Estimated cost of a line at the best known price (null when no price is known). */
    public static BigDecimal estimatedCost(PurchaseLine line) {
        BigDecimal price = line.bestUnitPrice() != null ? line.bestUnitPrice() : line.usualUnitPrice();
        return price == null ? null : price.multiply(line.suggestedQuantity()).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal suggestedQuantity(ItemForecast f, int horizon) {
        Item item = f.item();
        BigDecimal target = item.minStock().add(f.forecast().dailyRate().multiply(BigDecimal.valueOf(horizon)));
        BigDecimal missing = target.subtract(item.currentStock());
        if (missing.signum() <= 0) return BigDecimal.ZERO;
        // Shown in the item's unit: whole units for countable things, 3 decimals otherwise
        Unit unit = item.displayUnit();
        BigDecimal inUnit = unit.fromBase(missing);
        inUnit = item.dimension() == Dimension.COUNT
                ? inUnit.setScale(0, RoundingMode.CEILING)
                : inUnit.setScale(3, RoundingMode.CEILING);
        return unit.toBase(inUnit);
    }

    /** The cheapest supplier the user can choose; the usual one when the cheapest is someone else's. */
    private static UUID pickableSupplier(Offer best, Offer usual) {
        if (best != null && best.supplierId() != null) return best.supplierId();
        return usual == null ? null : usual.supplierId();
    }
}
