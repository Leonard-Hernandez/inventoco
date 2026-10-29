package com.lsoftware.inventoco.infrastructure.insight.controller;

import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import com.lsoftware.inventoco.infrastructure.insight.dto.ForecastResponse;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase;
import com.lsoftware.inventoco.usecase.insight.StatsUseCase;
import com.lsoftware.inventoco.usecase.insight.StatsUseCase.Savings;
import com.lsoftware.inventoco.usecase.insight.StatsUseCase.SupplierRank;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "Depletion forecasts, savings and supplier ranking")
@SecurityRequirement(name = "bearerAuth")
public class InsightController {

    private final CurrentUser currentUser;
    private final ForecastUseCase forecasts;
    private final StatsUseCase stats;

    @GetMapping("/forecasts")
    @Operation(summary = "Every item with when it runs out, soonest first")
    public List<ForecastResponse> forecasts(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam(defaultValue = "" + ShoppingListUseCase.DEFAULT_HORIZON_DAYS) int horizonDays) {
        return forecasts.all(currentUser.of(jwt), horizonDays).stream().map(ForecastResponse::from).toList();
    }

    @GetMapping("/savings")
    @Operation(summary = "Savings of the last days compared with the usual prices, and the estimate of the open list")
    public Savings savings(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "90") int days) {
        return stats.savings(currentUser.of(jwt), days);
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Suppliers from cheapest to most expensive (price index 0.9 = 10% under the typical price)")
    public List<SupplierRank> suppliers(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "180") int days) {
        return stats.supplierRanking(currentUser.of(jwt), days);
    }

    public record Dashboard(List<ForecastResponse> buySoon, Savings savings, List<SupplierRank> topSuppliers) {}

    @GetMapping("/dashboard")
    @Operation(summary = "What to buy soon, savings and the best suppliers, in one call")
    public Dashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        UserProfile user = currentUser.of(jwt);
        return new Dashboard(
                forecasts.all(user, ShoppingListUseCase.DEFAULT_HORIZON_DAYS).stream()
                        .filter(ForecastUseCase.ItemForecast::buySoon).map(ForecastResponse::from).toList(),
                stats.savings(user, 90),
                stats.supplierRanking(user, 180).stream().limit(5).toList());
    }
}
