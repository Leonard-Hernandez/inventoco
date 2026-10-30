package com.lsoftware.inventoco.infrastructure.item.controller;

import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import com.lsoftware.inventoco.infrastructure.insight.dto.ForecastResponse;
import com.lsoftware.inventoco.infrastructure.item.dto.ItemRequest;
import com.lsoftware.inventoco.infrastructure.item.dto.ItemResponse;
import com.lsoftware.inventoco.infrastructure.item.dto.MovementRequest;
import com.lsoftware.inventoco.infrastructure.item.dto.MovementResponse;
import com.lsoftware.inventoco.infrastructure.price.dto.PriceRequest;
import com.lsoftware.inventoco.infrastructure.price.dto.PriceResponse;
import com.lsoftware.inventoco.infrastructure.price.dto.PublicPriceResponse;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase;
import com.lsoftware.inventoco.usecase.price.PriceUseCase;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase;
import com.lsoftware.inventoco.usecase.supplier.ManageSupplierUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Inventory, stock movements, prices and forecast of each item")
@SecurityRequirement(name = "bearerAuth")
public class ItemController {

    private final CurrentUser currentUser;
    private final ManageItemUseCase items;
    private final RecordMovementUseCase movements;
    private final PriceUseCase prices;
    private final ForecastUseCase forecasts;
    private final ManageSupplierUseCase suppliers;

    @GetMapping
    @Operation(summary = "Active items, optionally filtered by text (name, category, barcode) or low stock")
    public List<ItemResponse> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "false") boolean lowStock) {
        return items.list(user(jwt).id(), q, lowStock).stream().map(ItemResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "New item; its unit fixes the dimension (count, mass, volume, length or area)")
    public ItemResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ItemRequest r) {
        return ItemResponse.from(items.create(user(jwt).id(), r.command()));
    }

    @GetMapping("/{id}")
    public ItemResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ItemResponse.from(items.get(user(jwt).id(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Change name, category, unit (same dimension), minimum stock, barcode or catalog product")
    public ItemResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ItemRequest r) {
        return ItemResponse.from(items.update(user(jwt).id(), id, r.command()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive (history is kept)")
    public void archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        items.archive(user(jwt).id(), id);
    }

    @GetMapping("/{id}/movements")
    public List<MovementResponse> movements(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                            @RequestParam(defaultValue = "100") int limit) {
        UserProfile user = user(jwt);
        Item item = items.get(user.id(), id);
        return movements.history(user.id(), id, limit).stream()
                .map(m -> MovementResponse.from(m, item.displayUnit())).toList();
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a purchase, consumption, waste or a count (ADJUSTMENT: quantity = counted stock)")
    public MovementResponse move(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody MovementRequest r) {
        UserProfile user = user(jwt);
        Item item = items.get(user.id(), id);
        return MovementResponse.from(movements.execute(user.id(), id, r.command()), item.displayUnit());
    }

    @GetMapping("/{id}/prices")
    @Operation(summary = "Price history of the item, newest first")
    public List<PriceResponse> prices(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UserProfile user = user(jwt);
        Item item = items.get(user.id(), id);
        return prices.history(user, id).stream()
                .map(e -> PriceResponse.from(e.price(), e.supplierName(), item.displayUnit())).toList();
    }

    @PostMapping("/{id}/prices")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a price seen or paid (PUBLIC shares it anonymously with other users)")
    public PriceResponse addPrice(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody PriceRequest r) {
        UserProfile user = user(jwt);
        Item item = items.get(user.id(), id);
        var saved = prices.record(user, id, r.command());
        return PriceResponse.from(saved, suppliers.usable(user.id(), saved.supplierId()).name(), item.displayUnit());
    }

    @GetMapping("/{id}/public-prices")
    @Operation(summary = "Prices other users shared for the same product, in your country and currency")
    public List<PublicPriceResponse> publicPrices(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UserProfile user = user(jwt);
        Item item = items.get(user.id(), id);
        return prices.publicForItem(user, id).stream()
                .map(p -> PublicPriceResponse.from(p, item.displayUnit())).toList();
    }

    @GetMapping("/{id}/forecast")
    @Operation(summary = "When the item runs out, from its recorded consumption")
    public ForecastResponse forecast(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @RequestParam(defaultValue = "" + ShoppingListUseCase.DEFAULT_HORIZON_DAYS) int horizonDays) {
        return ForecastResponse.from(forecasts.one(user(jwt), id, horizonDays));
    }

    private UserProfile user(Jwt jwt) {
        return currentUser.of(jwt);
    }
}
