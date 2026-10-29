package com.lsoftware.inventoco.usecase.price;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogGateway;
import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.exception.NotFoundException;
import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.price.gateway.PriceGateway;
import com.lsoftware.inventoco.entity.price.model.PriceObservation;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.entity.supplier.gateway.SupplierGateway;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import com.lsoftware.inventoco.usecase.supplier.ManageSupplierUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Price history per item and supplier, and the public prices other users shared. Prices are
 * compared only within the user's country and currency (no exchange rates).
 */
@Component
@RequiredArgsConstructor
public class PriceUseCase {

    /** How far back a price still counts as a current offer. */
    public static final Duration OFFER_WINDOW = Duration.ofDays(90);

    private final PriceGateway prices;
    private final SupplierGateway supplierGateway;
    private final CatalogGateway catalog;
    private final ManageItemUseCase items;
    private final ManageSupplierUseCase suppliers;
    private final Clock clock;

    /**
     * @param amount     paid for the whole package, in the user's currency
     * @param visibility null = the user's default
     */
    public record PriceCommand(UUID supplierId, BigDecimal amount, BigDecimal packageQuantity, String packageUnit,
                               Instant observedAt, Visibility visibility) {}

    @Transactional
    public PriceObservation record(UserProfile user, UUID itemId, PriceCommand c) {
        return record(user, items.get(user.id(), itemId), c, null);
    }

    /** Also used when a shopping list is completed (purchaseId links the price to it). */
    @Transactional
    public PriceObservation record(UserProfile user, Item item, PriceCommand c, UUID purchaseId) {
        if (c.supplierId() == null) throw new DomainException("INVALID_PRICE", "Choose the supplier");
        Supplier supplier = suppliers.usable(user.id(), c.supplierId());
        if (c.amount() == null || c.amount().signum() < 0) {
            throw new DomainException("INVALID_PRICE", "The price cannot be negative");
        }
        if (c.packageQuantity() == null || c.packageQuantity().signum() <= 0) {
            throw new DomainException("INVALID_QUANTITY", "The package quantity must be greater than zero");
        }
        Unit unit = c.packageUnit() == null || c.packageUnit().isBlank()
                ? item.displayUnit() : Unit.parse(c.packageUnit(), item.dimension());
        BigDecimal base = unit.toBase(c.packageQuantity());
        Instant now = clock.instant();
        Instant observedAt = c.observedAt() == null ? now : c.observedAt();
        if (observedAt.isAfter(now.plusSeconds(300))) {
            throw new DomainException("INVALID_DATE", "Prices cannot be observed in the future");
        }
        Visibility visibility = c.visibility() != null ? c.visibility()
                : user.sharePricesByDefault() ? Visibility.PUBLIC : Visibility.PRIVATE;
        // Without a catalog product nobody else could find the price, so it stays private
        if (item.catalogProductId() == null) visibility = Visibility.PRIVATE;
        return prices.save(new PriceObservation(null, user.id(), item.id(), supplier.id(), item.catalogProductId(),
                c.amount(), user.currency(), base, unit, PriceObservation.unitPrice(c.amount(), base),
                supplier.country(), visibility, observedAt, purchaseId, now));
    }

    public record PriceEntry(PriceObservation price, String supplierName) {}

    /** The user's prices of an item, newest first. */
    public List<PriceEntry> history(UserProfile user, UUID itemId) {
        items.get(user.id(), itemId);
        List<PriceObservation> list = prices.listByItem(user.id(), itemId);
        Map<UUID, Supplier> bySupplier = suppliersOf(list);
        return list.stream().map(p -> new PriceEntry(p, Optional.ofNullable(bySupplier.get(p.supplierId()))
                .map(Supplier::name).orElse("?"))).toList();
    }

    /** Public prices of the item's catalog product in the user's country and currency. */
    public List<PublicPrice> publicForItem(UserProfile user, UUID itemId) {
        Item item = items.get(user.id(), itemId);
        if (item.catalogProductId() == null) return List.of();
        return publicForProduct(user, item.catalogProductId());
    }

    public List<PublicPrice> publicForProduct(UserProfile user, UUID catalogProductId) {
        catalog.findById(catalogProductId)
                .orElseThrow(() -> new NotFoundException("CATALOG_PRODUCT_NOT_FOUND", "Catalog product not found"));
        List<PriceObservation> list = prices.listPublic(List.of(catalogProductId), user.country(), user.currency(),
                clock.instant().minus(OFFER_WINDOW));
        Map<UUID, Supplier> bySupplier = suppliersOf(list);
        return list.stream().map(p -> toPublic(p, bySupplier.get(p.supplierId()))).toList();
    }

    /** Cheapest current offer and the price usually paid, for each item. */
    public Map<UUID, ItemOffers> offers(UserProfile user, Collection<Item> forItems) {
        Instant since = clock.instant().minus(OFFER_WINDOW);
        List<PriceObservation> own = prices.listByOwnerSince(user.id(), since);
        List<UUID> productIds = forItems.stream().map(Item::catalogProductId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        List<PriceObservation> publics = productIds.isEmpty() ? List.of()
                : prices.listPublic(productIds, user.country(), user.currency(), since);
        Map<UUID, Supplier> bySupplier = suppliersOf(concat(own, publics));

        Map<UUID, ItemOffers> result = new HashMap<>();
        for (Item item : forItems) {
            List<PriceObservation> mine = own.stream()
                    .filter(p -> item.id().equals(p.itemId()) && user.currency().equals(p.currency())).toList();
            Optional<Offer> usual = mine.stream().max(Comparator.comparing(PriceObservation::observedAt))
                    .map(p -> offer(p, bySupplier, user, Offer.Source.OWN));
            Optional<Offer> best = concat(
                    mine.stream().map(p -> offer(p, bySupplier, user, Offer.Source.OWN)).toList(),
                    publics.stream()
                            .filter(p -> item.catalogProductId() != null && item.catalogProductId().equals(p.catalogProductId())
                                    && !user.id().equals(p.ownerId()))
                            .map(p -> offer(p, bySupplier, user, Offer.Source.PUBLIC)).toList())
                    .stream().min(Comparator.comparing(Offer::unitPrice));
            result.put(item.id(), new ItemOffers(best.orElse(null), usual.orElse(null)));
        }
        return result;
    }

    public record ItemOffers(Offer best, Offer usual) {}

    private static Offer offer(PriceObservation p, Map<UUID, Supplier> suppliers, UserProfile user, Offer.Source source) {
        Supplier s = suppliers.get(p.supplierId());
        String name = s == null ? "?" : s.name();
        UUID pickable = s != null && s.usableBy(user.id()) ? s.id() : null;
        return new Offer(pickable, name, p.unitPrice(), p.observedAt(), source);
    }

    static PublicPrice toPublic(PriceObservation p, Supplier s) {
        return new PublicPrice(s == null ? "?" : s.name(), s == null ? null : s.city(), p.country(), p.amount(),
                p.currency(), p.packageUnit().fromBase(p.packageQuantity()), p.packageUnit().symbol(), p.unitPrice(),
                p.observedAt(), s != null && s.system());
    }

    private Map<UUID, Supplier> suppliersOf(List<PriceObservation> list) {
        List<UUID> ids = list.stream().map(PriceObservation::supplierId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return supplierGateway.findAllById(ids).stream().collect(Collectors.toMap(Supplier::id, Function.identity()));
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }
}
