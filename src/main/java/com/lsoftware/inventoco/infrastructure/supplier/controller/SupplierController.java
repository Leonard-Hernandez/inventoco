package com.lsoftware.inventoco.infrastructure.supplier.controller;

import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import com.lsoftware.inventoco.infrastructure.supplier.dto.SupplierRequest;
import com.lsoftware.inventoco.infrastructure.supplier.dto.SupplierResponse;
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
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Where the user buys; store chains of their country are listed too")
@SecurityRequirement(name = "bearerAuth")
public class SupplierController {

    private final CurrentUser currentUser;
    private final ManageSupplierUseCase suppliers;

    @GetMapping
    @Operation(summary = "The user's suppliers, then the store chains of their country")
    public List<SupplierResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return suppliers.list(currentUser.of(jwt)).stream().map(SupplierResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SupplierRequest r) {
        return SupplierResponse.from(suppliers.create(currentUser.of(jwt), r.command()));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody SupplierRequest r) {
        return SupplierResponse.from(suppliers.update(currentUser.of(jwt), id, r.command()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive (its past prices are kept)")
    public void archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        suppliers.archive(currentUser.of(jwt), id);
    }
}
