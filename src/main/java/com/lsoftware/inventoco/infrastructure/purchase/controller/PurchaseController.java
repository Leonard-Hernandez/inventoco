package com.lsoftware.inventoco.infrastructure.purchase.controller;

import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.entity.purchase.model.Purchase;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseLine;
import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import com.lsoftware.inventoco.infrastructure.purchase.dto.BoughtRequest;
import com.lsoftware.inventoco.infrastructure.purchase.dto.PurchaseResponse;
import com.lsoftware.inventoco.usecase.item.ManageItemUseCase;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@Tag(name = "Shopping", description = "What's missing, where it is cheapest, and what was bought")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseController {

    private final CurrentUser currentUser;
    private final ShoppingListUseCase shopping;
    private final ManageItemUseCase items;

    @GetMapping("/draft")
    @Operation(summary = "The open shopping list (204 when there is none)")
    public ResponseEntity<PurchaseResponse> draft(@AuthenticationPrincipal Jwt jwt) {
        UserProfile user = currentUser.of(jwt);
        return shopping.draft(user).map(p -> ResponseEntity.ok(response(user, p)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/generate")
    @Operation(summary = "Build the shopping list: low stock plus what runs out within horizonDays (replaces the open one)")
    public PurchaseResponse generate(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) Integer horizonDays) {
        UserProfile user = currentUser.of(jwt);
        return response(user, shopping.generate(user, horizonDays));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark the list as bought: adds the stock and records the prices paid")
    public PurchaseResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @Valid @RequestBody BoughtRequest r) {
        UserProfile user = currentUser.of(jwt);
        return response(user, shopping.complete(user, id, r.bought(), r.occurredAt()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a purchase made without a shopping list")
    public PurchaseResponse record(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BoughtRequest r) {
        UserProfile user = currentUser.of(jwt);
        return response(user, shopping.recordPurchase(user, r.bought(), r.occurredAt()));
    }

    @GetMapping
    @Operation(summary = "Completed purchases, newest first")
    public List<PurchaseResponse> history(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "20") int limit) {
        UserProfile user = currentUser.of(jwt);
        return shopping.history(user, limit).stream().map(p -> response(user, p)).toList();
    }

    private PurchaseResponse response(UserProfile user, Purchase p) {
        return PurchaseResponse.from(p, items.byIds(user.id(), p.lines().stream().map(PurchaseLine::itemId).toList()));
    }
}
