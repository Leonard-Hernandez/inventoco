package com.lsoftware.inventoco.infrastructure.profile.controller;

import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import com.lsoftware.inventoco.infrastructure.profile.dto.ProfileRequest;
import com.lsoftware.inventoco.infrastructure.profile.dto.ProfileResponse;
import com.lsoftware.inventoco.usecase.profile.UpdateProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Country, currency and time zone of the signed-in user")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final CurrentUser currentUser;
    private final UpdateProfileUseCase updateProfile;

    @GetMapping("/me")
    @Operation(summary = "Profile of the signed-in user (created on the first visit)")
    public ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(currentUser.of(jwt));
    }

    @PutMapping("/me")
    @Operation(summary = "Change country, currency (defaults to the country's) and time zone")
    public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest r) {
        return ProfileResponse.from(updateProfile.execute(currentUser.of(jwt), r.country(), r.currency(),
                r.timeZone(), r.sharePricesByDefault()));
    }

    public record UnitResponse(String symbol, String dimension, BigDecimal factor, boolean base) {}

    @GetMapping("/units")
    @Operation(summary = "Units that can be typed, with their factor to the base unit (unit, g, ml)")
    public List<UnitResponse> units() {
        return Arrays.stream(Unit.values())
                .map(u -> new UnitResponse(u.symbol(), u.dimension().name(), u.factor(), u.dimension().baseUnit() == u))
                .toList();
    }
}
