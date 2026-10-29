package com.lsoftware.inventoco.infrastructure.mcp;

import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.entity.purchase.model.Purchase;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseLine;
import com.lsoftware.inventoco.infrastructure.insight.dto.ForecastResponse;
import com.lsoftware.inventoco.infrastructure.item.dto.ItemResponse;
import com.lsoftware.inventoco.infrastructure.item.dto.MovementResponse;
import com.lsoftware.inventoco.infrastructure.price.dto.PriceResponse;
import com.lsoftware.inventoco.infrastructure.price.dto.PublicPriceResponse;
import com.lsoftware.inventoco.infrastructure.profile.dto.ProfileResponse;
import com.lsoftware.inventoco.infrastructure.purchase.dto.PurchaseResponse;
import com.lsoftware.inventoco.infrastructure.supplier.dto.SupplierResponse;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase;
import com.lsoftware.inventoco.usecase.insight.StatsUseCase;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase.ItemCommand;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase.MovementCommand;
import com.lsoftware.inventoco.usecase.price.PriceUseCase;
import com.lsoftware.inventoco.usecase.price.PriceUseCase.PriceCommand;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase.BoughtLine;
import com.lsoftware.inventoco.usecase.supplier.ManageSupplierUseCase;
import com.lsoftware.inventoco.usecase.supplier.ManageSupplierUseCase.SupplierCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tools over the caller's inventory. They call the same use cases as the web app, so the same
 * rules apply (ownership, units of the right kind, no negative stock).
 */
@Component
@RequiredArgsConstructor
public class InventoryTools {

    private static final String QUANTITY_UNIT = "Unit of the quantity (kg, g, lb, oz, l, ml, gal, unit, dozen...); defaults to the item's unit";

    private final ManageItemUseCase items;
    private final RecordMovementUseCase movements;
    private final ManageSupplierUseCase suppliers;
    private final PriceUseCase prices;
    private final ShoppingListUseCase shopping;
    private final ForecastUseCase forecasts;
    private final StatsUseCase stats;

    public List<McpTool> tools() {
        return List.of(
                new McpTool("get_profile", "User profile",
                        "Country, currency (every amount is in it) and time zone of the user.",
                        Schemas.object().build(), true,
                        (a, user) -> ProfileResponse.from(user)),
                new McpTool("list_items", "List items",
                        "The user's items with their stock and minimum, in each item's unit. Use it to find item ids.",
                        Schemas.object()
                                .string("query", "Part of the name, the category or the barcode", false)
                                .bool("low_stock_only", "Only items at or below their minimum stock", false)
                                .build(), true,
                        (a, user) -> items.list(user.id(), a.string("query"), a.bool("low_stock_only", false))
                                .stream().map(ItemResponse::from).toList()),
                new McpTool("get_item", "Item detail",
                        "One item: stock, forecast of when it runs out, last movements and last prices paid.",
                        Schemas.object().id("item_id", "Item id (from list_items)", true).build(), true,
                        (a, user) -> itemDetail(user, a.requiredId("item_id"))),
                new McpTool("create_item", "Create item",
                        "Adds an item to the inventory. Its unit fixes how it is measured (count, mass or volume).",
                        Schemas.object()
                                .string("name", "Name, e.g. 'Arroz Diana'", true)
                                .string("unit", "Unit it is counted in: unit, kg, g, l, ml...", true)
                                .number("min_stock", "Minimum stock before it should be bought, in that unit", false)
                                .number("initial_stock", "Stock right now, in that unit", false)
                                .string("category", "Category, e.g. 'Despensa', 'Aseo'", false)
                                .string("barcode", "Barcode (EAN/UPC) if known: links it to the shared catalog", false)
                                .build(), false,
                        (a, user) -> ItemResponse.from(items.create(user.id(), new ItemCommand(a.requiredString("name"),
                                a.string("category"), a.requiredString("unit"), a.decimal("min_stock"),
                                a.decimal("initial_stock"), a.string("barcode"), null)))),
                new McpTool("record_consumption", "Record consumption",
                        "Records that some of an item was used (or thrown away). Feeds the depletion forecast.",
                        Schemas.object()
                                .id("item_id", "Item id", true)
                                .number("quantity", "How much was used", true)
                                .string("unit", QUANTITY_UNIT, false)
                                .enumeration("kind", "CONSUMPTION (default) or WASTE (expired, broken)",
                                        List.of("CONSUMPTION", "WASTE"), false)
                                .dateTime("occurred_at", "When (default now), e.g. 2026-10-21 or 2026-10-21T19:00", false)
                                .string("note", "Optional note", false)
                                .build(), false,
                        (a, user) -> movement(user, a, "WASTE".equals(a.string("kind")) ? MovementType.WASTE : MovementType.CONSUMPTION)),
                new McpTool("count_stock", "Set counted stock",
                        "Corrects the stock of an item to what was physically counted.",
                        Schemas.object()
                                .id("item_id", "Item id", true)
                                .number("quantity", "The stock counted", true)
                                .string("unit", QUANTITY_UNIT, false)
                                .build(), false,
                        (a, user) -> movement(user, a, MovementType.ADJUSTMENT)),
                new McpTool("record_purchase", "Record purchase",
                        "Records that an item was bought: adds the stock and, with supplier and amount, the price paid.",
                        Schemas.object()
                                .id("item_id", "Item id", true)
                                .number("quantity", "How much was bought", true)
                                .string("unit", QUANTITY_UNIT, false)
                                .id("supplier_id", "Where (from list_suppliers)", false)
                                .number("amount", "Total paid for that quantity, in the user's currency", false)
                                .bool("share_publicly", "Share the price anonymously with other users (default: user's setting)", false)
                                .dateTime("occurred_at", "When (default now)", false)
                                .build(), false,
                        (a, user) -> {
                            Purchase p = shopping.recordPurchase(user, List.of(new BoughtLine(a.requiredId("item_id"),
                                    a.requiredDecimal("quantity"), a.string("unit"), a.id("supplier_id"),
                                    a.decimal("amount"), visibility(a))), a.instant("occurred_at", user.timeZone()));
                            return Map.of("purchaseId", p.id(),
                                    "item", ItemResponse.from(items.get(user.id(), a.requiredId("item_id"))));
                        }),
                new McpTool("record_price", "Record price",
                        "Records a price seen at a supplier without buying (e.g. a shelf price or a flyer).",
                        Schemas.object()
                                .id("item_id", "Item id", true)
                                .id("supplier_id", "Supplier id (from list_suppliers)", true)
                                .number("amount", "Price of the package, in the user's currency", true)
                                .number("package_quantity", "Size of the package, e.g. 500", true)
                                .string("package_unit", "Unit of the package size, e.g. g; defaults to the item's", false)
                                .bool("share_publicly", "Share it anonymously with other users (default: user's setting)", false)
                                .dateTime("observed_at", "When it was seen (default now)", false)
                                .build(), false,
                        (a, user) -> PriceResponse.from(prices.record(user, a.requiredId("item_id"), new PriceCommand(
                                a.requiredId("supplier_id"), a.requiredDecimal("amount"),
                                a.requiredDecimal("package_quantity"), a.string("package_unit"),
                                a.instant("observed_at", user.timeZone()), visibility(a))),
                                suppliers.usable(user.id(), a.requiredId("supplier_id")).name(),
                                items.get(user.id(), a.requiredId("item_id")).displayUnit())),
                new McpTool("list_suppliers", "List suppliers",
                        "The user's suppliers and the store chains of their country, with their ids.",
                        Schemas.object().build(), true,
                        (a, user) -> suppliers.list(user).stream().map(SupplierResponse::from).toList()),
                new McpTool("create_supplier", "Create supplier",
                        "Adds a supplier (shop, market, distributor) of the user.",
                        Schemas.object()
                                .string("name", "Name", true)
                                .string("city", "City", false)
                                .string("url", "Website", false)
                                .build(), false,
                        (a, user) -> SupplierResponse.from(suppliers.create(user, new SupplierCommand(
                                a.requiredString("name"), null, a.string("city"), a.string("url"))))),
                new McpTool("price_history", "Price history",
                        "Every price the user recorded for an item, newest first, with the price per item unit.",
                        Schemas.object().id("item_id", "Item id", true).build(), true,
                        (a, user) -> {
                            Item item = items.get(user.id(), a.requiredId("item_id"));
                            return prices.history(user, item.id()).stream()
                                    .map(e -> PriceResponse.from(e.price(), e.supplierName(), item.displayUnit())).toList();
                        }),
                new McpTool("public_prices", "Public prices",
                        "Prices other users (and store listings) shared for the same product in the user's country and "
                                + "currency, over the last 90 days. Anonymous.",
                        Schemas.object().id("item_id", "Item id", true).build(), true,
                        (a, user) -> {
                            Item item = items.get(user.id(), a.requiredId("item_id"));
                            return prices.publicForItem(user, item.id()).stream()
                                    .map(p -> PublicPriceResponse.from(p, item.displayUnit())).toList();
                        }),
                new McpTool("forecast", "Depletion forecast",
                        "When each item runs out, from its recorded consumption, soonest first. buySoon = low stock or "
                                + "runs out within the horizon. Confidence is LOW with few records.",
                        Schemas.object()
                                .integer("horizon_days", "Days ahead to consider (default 7)", false)
                                .bool("buy_soon_only", "Only items to buy soon", false)
                                .build(), true,
                        (a, user) -> forecasts.all(user, horizon(a)).stream()
                                .filter(f -> !a.bool("buy_soon_only", false) || f.buySoon())
                                .map(ForecastResponse::from).toList()),
                new McpTool("generate_shopping_list", "Generate shopping list",
                        "Builds the shopping list (replacing the open one): items at or below their minimum and those "
                                + "running out within the horizon, with suggested quantity, cheapest supplier and estimated savings.",
                        Schemas.object().integer("horizon_days", "Days to cover (default 7)", false).build(), false,
                        (a, user) -> purchase(user, shopping.generate(user, a.integer("horizon_days")))),
                new McpTool("get_shopping_list", "Open shopping list",
                        "The open shopping list, if any.",
                        Schemas.object().build(), true,
                        (a, user) -> shopping.draft(user).map(p -> (Object) purchase(user, p))
                                .orElse(Map.of("message", "There is no open shopping list"))),
                new McpTool("savings_summary", "Savings",
                        "Savings over the last days compared with the usual price of each item, money spent, and the "
                                + "estimated savings of the open shopping list.",
                        Schemas.object().integer("days", "Period in days (default 90)", false).build(), true,
                        (a, user) -> stats.savings(user, a.integer("days") == null ? 90 : a.integer("days"))),
                new McpTool("best_suppliers", "Best suppliers",
                        "The user's suppliers from cheapest to most expensive: price index (0.9 = 10% under the typical "
                                + "price of what they sell) and in how many items each one is the cheapest.",
                        Schemas.object().integer("days", "Period in days (default 180)", false).build(), true,
                        (a, user) -> stats.supplierRanking(user, a.integer("days") == null ? 180 : a.integer("days"))));
    }

    private Object itemDetail(UserProfile user, java.util.UUID itemId) {
        Item item = items.get(user.id(), itemId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("item", ItemResponse.from(item));
        detail.put("forecast", ForecastResponse.from(forecasts.one(user, itemId, ShoppingListUseCase.DEFAULT_HORIZON_DAYS)));
        detail.put("lastMovements", movements.history(user.id(), itemId, 10).stream()
                .map(m -> MovementResponse.from(m, item.displayUnit())).toList());
        detail.put("lastPrices", prices.history(user, itemId).stream().limit(5)
                .map(e -> PriceResponse.from(e.price(), e.supplierName(), item.displayUnit())).toList());
        return detail;
    }

    private Object movement(UserProfile user, McpArgs a, MovementType type) {
        Item item = items.get(user.id(), a.requiredId("item_id"));
        var saved = movements.execute(user.id(), item.id(), new MovementCommand(type, a.requiredDecimal("quantity"),
                a.string("unit"), a.instant("occurred_at", user.timeZone()), a.string("note")));
        return Map.of("movement", MovementResponse.from(saved, item.displayUnit()),
                "item", ItemResponse.from(items.get(user.id(), item.id())));
    }

    private PurchaseResponse purchase(UserProfile user, Purchase p) {
        return PurchaseResponse.from(p, items.byIds(user.id(), p.lines().stream().map(PurchaseLine::itemId).toList()));
    }

    private static Visibility visibility(McpArgs a) {
        if (a.values().get("share_publicly") == null) return null;
        return a.bool("share_publicly", false) ? Visibility.PUBLIC : Visibility.PRIVATE;
    }

    private static int horizon(McpArgs a) {
        Integer h = a.integer("horizon_days");
        return h == null ? ShoppingListUseCase.DEFAULT_HORIZON_DAYS : Math.clamp(h, 1, 90);
    }
}
