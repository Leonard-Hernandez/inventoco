package com.lsoftware.inventoco.usecase.insight;

import com.lsoftware.inventoco.entity.price.gateway.PriceGateway;
import com.lsoftware.inventoco.entity.price.model.PriceObservation;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.entity.supplier.gateway.SupplierGateway;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Savings and supplier ranking from the user's own price history (in their currency only).
 *
 * <ul>
 *   <li>Realized savings of a purchase line = (the item's average unit price over its history, bought
 *       or seen − the unit price paid) × the quantity bought. Negative when the user paid more than
 *       usual.</li>
 *   <li>Supplier ranking: each price is divided by the median unit price of its item, so a supplier's
 *       index is how much it charges compared with the typical price (0.9 = 10% cheaper).</li>
 *   <li>Estimated savings: of the open shopping list, buying each line at the cheapest known price
 *       instead of the usual one.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StatsUseCase {

    private final PriceGateway prices;
    private final SupplierGateway suppliers;
    private final ShoppingListUseCase shopping;
    private final Clock clock;

    public record Savings(String currency, int days, BigDecimal realized, BigDecimal spent, int purchases,
                          BigDecimal estimatedOnShoppingList) {}

    public record SupplierRank(UUID supplierId, String name, String city, BigDecimal priceIndex, int prices,
                               int itemsCheapest) {}

    public Savings savings(UserProfile user, int days) {
        int d = Math.clamp(days, 1, 3650);
        Instant since = clock.instant().minus(Duration.ofDays(d));
        // Averages use all history, savings only count the period
        List<PriceObservation> all = ownInCurrency(user, Instant.EPOCH);
        Map<UUID, BigDecimal> average = all.stream().filter(p -> p.itemId() != null).collect(Collectors.groupingBy(
                PriceObservation::itemId, Collectors.collectingAndThen(Collectors.toList(), StatsUseCase::mean)));
        BigDecimal realized = BigDecimal.ZERO;
        BigDecimal spent = BigDecimal.ZERO;
        int count = 0;
        for (PriceObservation p : all) {
            // Only what was bought counts, not prices merely seen on a shelf
            if (p.observedAt().isBefore(since) || p.purchaseId() == null || p.itemId() == null) continue;
            BigDecimal avg = average.get(p.itemId());
            realized = realized.add(avg.subtract(p.unitPrice()).multiply(p.packageQuantity()));
            spent = spent.add(p.amount());
            count++;
        }
        BigDecimal estimated = shopping.draft(user)
                .map(list -> list.lines().stream().map(ShoppingListUseCase::estimatedSavings)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);
        return new Savings(user.currency(), d, realized.setScale(2, RoundingMode.HALF_UP),
                spent.setScale(2, RoundingMode.HALF_UP), count, estimated);
    }

    /** Cheapest suppliers first (lowest price index). */
    public List<SupplierRank> supplierRanking(UserProfile user, int days) {
        Instant since = clock.instant().minus(Duration.ofDays(Math.clamp(days, 1, 3650)));
        List<PriceObservation> list = ownInCurrency(user, since).stream().filter(p -> p.itemId() != null).toList();
        if (list.isEmpty()) return List.of();
        Map<UUID, List<PriceObservation>> byItem = list.stream().collect(Collectors.groupingBy(PriceObservation::itemId));
        Map<UUID, BigDecimal> median = new HashMap<>();
        byItem.forEach((item, ps) -> median.put(item, median(ps)));

        // Cheapest supplier of each item: the one with the lowest latest price among the suppliers used
        Map<UUID, Integer> cheapest = new HashMap<>();
        byItem.forEach((item, ps) -> ps.stream()
                .collect(Collectors.groupingBy(PriceObservation::supplierId,
                        Collectors.maxBy(Comparator.comparing(PriceObservation::observedAt))))
                .values().stream().flatMap(java.util.Optional::stream)
                .min(Comparator.comparing(PriceObservation::unitPrice))
                .ifPresent(p -> cheapest.merge(p.supplierId(), 1, Integer::sum)));

        Map<UUID, Supplier> bySupplier = suppliers.findAllById(list.stream().map(PriceObservation::supplierId)
                .distinct().toList()).stream().collect(Collectors.toMap(Supplier::id, Function.identity()));
        List<SupplierRank> ranks = new ArrayList<>();
        list.stream().collect(Collectors.groupingBy(PriceObservation::supplierId)).forEach((supplierId, ps) -> {
            BigDecimal index = ps.stream()
                    .map(p -> median.get(p.itemId()).signum() == 0 ? BigDecimal.ONE
                            : p.unitPrice().divide(median.get(p.itemId()), MathContext.DECIMAL64))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(ps.size()), 3, RoundingMode.HALF_UP);
            Supplier s = bySupplier.get(supplierId);
            ranks.add(new SupplierRank(supplierId, s == null ? "?" : s.name(), s == null ? null : s.city(), index,
                    ps.size(), cheapest.getOrDefault(supplierId, 0)));
        });
        ranks.sort(Comparator.comparing(SupplierRank::priceIndex).thenComparing(SupplierRank::prices, Comparator.reverseOrder()));
        return ranks;
    }

    private List<PriceObservation> ownInCurrency(UserProfile user, Instant since) {
        return prices.listByOwnerSince(user.id(), since).stream()
                .filter(p -> user.currency().equals(p.currency())).toList();
    }

    private static BigDecimal mean(List<PriceObservation> ps) {
        return ps.stream().map(PriceObservation::unitPrice).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ps.size()), PriceObservation.UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal median(List<PriceObservation> ps) {
        List<BigDecimal> sorted = ps.stream().map(PriceObservation::unitPrice).sorted().toList();
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2))
                .divide(BigDecimal.TWO, PriceObservation.UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
    }
}
