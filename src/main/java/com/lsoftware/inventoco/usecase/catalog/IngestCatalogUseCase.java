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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Seeds the shared catalog and the public prices from store chains. Each listing becomes (or
 * matches, by barcode or else by name) a catalog product, and its shelf price a public price of the chain as a
 * system supplier: no user owns it. A price is stored again only when it changed or a day passed,
 * so repeated runs build a price history without piling up copies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestCatalogUseCase {

    static final Duration REFRESH = Duration.ofHours(20);

    private final CatalogGateway catalog;
    private final SupplierGateway suppliers;
    private final PriceGateway prices;
    private final Clock clock;

    public record Result(int listings, int newProducts, int prices) {}

    public Result run(CatalogSource source, List<String> terms, int perTerm) {
        int listings = 0, products = 0, stored = 0;
        for (String term : terms) {
            List<StoreListing> found;
            try {
                found = source.search(term, perTerm);
            } catch (RuntimeException e) {
                log.warn("Catalog ingest: {} search '{}' failed: {}", source.chain(), term, e.getMessage());
                continue;
            }
            for (StoreListing l : found) {
                listings++;
                Outcome o = ingest(source, l);
                if (o.newProduct()) products++;
                if (o.priceStored()) stored++;
            }
        }
        log.info("Catalog ingest from {}: {} listings, {} new products, {} prices", source.chain(), listings, products, stored);
        return new Result(listings, products, stored);
    }

    record Outcome(boolean newProduct, boolean priceStored) {}

    /** Each write commits on its own: one bad listing never undoes the rest of a run. */
    Outcome ingest(CatalogSource source, StoreListing l) {
        if (l.name() == null || l.name().isBlank() || l.price() == null || l.price().signum() <= 0
                || l.packageQuantity() == null || l.packageQuantity().signum() <= 0 || l.packageUnit() == null) {
            return new Outcome(false, false);
        }
        Instant now = clock.instant();
        // By barcode when the store gives one; otherwise by name (fresh produce: "Banano", "Papa capira")
        String gtin = CatalogProduct.gtin(l.gtin());
        Optional<CatalogProduct> existing = gtin != null ? catalog.findByGtin(gtin)
                : catalog.findByNormalizedName(CatalogProduct.normalize(l.name()));
        if (existing.isPresent() && existing.get().dimension() != l.packageUnit().dimension()) {
            return new Outcome(false, false);
        }
        CatalogProduct product = existing.orElseGet(() ->
                catalog.save(CatalogProduct.create(l.name(), l.brand(), gtin, l.packageUnit().dimension(), now)));
        Supplier chain = suppliers.findSystem(source.country(), source.chain()).orElseGet(() ->
                suppliers.save(new Supplier(null, null, source.chain(), source.country(), null, null, false, now)));

        BigDecimal base = l.packageUnit().toBase(l.packageQuantity());
        BigDecimal unitPrice = PriceObservation.unitPrice(l.price(), base);
        Optional<PriceObservation> last = prices.latestAt(chain.id(), product.id());
        boolean fresh = last.isPresent() && last.get().unitPrice().compareTo(unitPrice) == 0
                && last.get().observedAt().isAfter(now.minus(REFRESH));
        if (!fresh) {
            prices.save(new PriceObservation(null, null, null, chain.id(), product.id(), l.price(), l.currency(), base,
                    l.packageUnit(), unitPrice, source.country(), Visibility.PUBLIC, now, null, now));
        }
        return new Outcome(existing.isEmpty(), !fresh);
    }
}
