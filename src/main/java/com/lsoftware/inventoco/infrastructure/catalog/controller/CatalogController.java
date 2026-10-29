package com.lsoftware.inventoco.infrastructure.catalog.controller;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogGateway;
import com.lsoftware.inventoco.entity.catalog.model.CatalogProduct;
import com.lsoftware.inventoco.entity.exception.NotFoundException;
import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import com.lsoftware.inventoco.infrastructure.price.dto.PublicPriceResponse;
import com.lsoftware.inventoco.usecase.price.PriceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Products shared by every user, and their public prices")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {

    private final CatalogGateway catalog;
    private final PriceUseCase prices;
    private final CurrentUser currentUser;

    public record CatalogProductResponse(UUID id, String name, String brand, String gtin, String dimension) {
        static CatalogProductResponse from(CatalogProduct p) {
            return new CatalogProductResponse(p.id(), p.name(), p.brand(), p.gtin(), p.dimension().name());
        }
    }

    @GetMapping
    @Operation(summary = "Search by name (accents and case ignored) or barcode")
    public List<CatalogProductResponse> search(@RequestParam String q) {
        String gtin = CatalogProduct.gtin(q);
        if (gtin != null) return catalog.findByGtin(gtin).map(CatalogProductResponse::from).stream().toList();
        String text = CatalogProduct.normalize(q);
        if (text.length() < 2) return List.of();
        return catalog.search(text, 20).stream().map(CatalogProductResponse::from).toList();
    }

    @GetMapping("/{id}/prices")
    @Operation(summary = "Public prices of a product in your country and currency (per base unit: unit, g or ml)")
    public List<PublicPriceResponse> prices(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        CatalogProduct product = catalog.findById(id)
                .orElseThrow(() -> new NotFoundException("CATALOG_PRODUCT_NOT_FOUND", "Catalog product not found"));
        return prices.publicForProduct(currentUser.of(jwt), id).stream()
                .map(p -> PublicPriceResponse.from(p, product.dimension().baseUnit())).toList();
    }
}
