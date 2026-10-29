package com.lsoftware.inventoco.usecase.insight;

import com.lsoftware.inventoco.entity.item.gateway.MovementGateway;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.item.model.StockMovement;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Depletion forecasts of the user's items, soonest first. */
@Component
@RequiredArgsConstructor
public class ForecastUseCase {

    private final ManageItemUseCase items;
    private final MovementGateway movements;
    private final Clock clock;

    public record ItemForecast(Item item, DepletionForecast.Result forecast, boolean buySoon) {}

    /** @param horizonDays an item is "buy soon" when it runs out within this many days, or is low */
    public List<ItemForecast> all(UserProfile user, int horizonDays) {
        return forecast(user, items.list(user.id(), null, false), horizonDays);
    }

    public ItemForecast one(UserProfile user, UUID itemId, int horizonDays) {
        return forecast(user, List.of(items.get(user.id(), itemId)), horizonDays).getFirst();
    }

    public List<ItemForecast> forecast(UserProfile user, List<Item> list, int horizonDays) {
        Instant now = clock.instant();
        Map<UUID, List<StockMovement>> byItem = movements
                .outflowsSince(user.id(), now.minus(Duration.ofDays(DepletionForecast.WINDOW_DAYS + 1)))
                .stream().collect(Collectors.groupingBy(StockMovement::itemId));
        BigDecimal horizon = BigDecimal.valueOf(horizonDays);
        return list.stream()
                .map(item -> {
                    DepletionForecast.Result f = DepletionForecast.forecast(item.currentStock(),
                            byItem.getOrDefault(item.id(), List.of()), now, user.timeZone());
                    boolean soon = item.lowStock() || (f.daysLeft() != null && f.daysLeft().compareTo(horizon) <= 0);
                    return new ItemForecast(item, f, soon);
                })
                .sorted(Comparator.comparing((ItemForecast f) -> f.forecast().daysLeft(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
