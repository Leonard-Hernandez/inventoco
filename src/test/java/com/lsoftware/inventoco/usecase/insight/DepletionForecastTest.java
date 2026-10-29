package com.lsoftware.inventoco.usecase.insight;

import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.entity.item.model.StockMovement;
import com.lsoftware.inventoco.usecase.insight.DepletionForecast.Confidence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.lsoftware.inventoco.usecase.Fixtures.BOGOTA;
import static com.lsoftware.inventoco.usecase.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

class DepletionForecastTest {

    @Test
    void steadyDailyConsumptionGivesThatRateAndTheDaysTheStockLasts() {
        List<StockMovement> used = new ArrayList<>();
        for (int d = 0; d < 30; d++) used.add(outflow(NOW.minus(Duration.ofDays(d)), "100"));

        var f = DepletionForecast.forecast(new BigDecimal("1000"), used, NOW, BOGOTA);

        assertThat(f.dailyRate()).isEqualByComparingTo("100");
        assertThat(f.daysLeft()).isEqualByComparingTo("10.0");
        assertThat(f.depletesAt()).isEqualTo(NOW.plus(Duration.ofDays(10)));
        assertThat(f.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(f.samples()).isEqualTo(30);
    }

    @Test
    void sparseRecordsAverageOverTheDaysInBetween() {
        // 700 g once a week for three weeks: about 100 g a day
        var f = DepletionForecast.forecast(new BigDecimal("700"), List.of(
                outflow(NOW.minus(Duration.ofDays(20)), "700"),
                outflow(NOW.minus(Duration.ofDays(13)), "700"),
                outflow(NOW.minus(Duration.ofDays(6)), "700")), NOW, BOGOTA);

        assertThat(f.dailyRate().doubleValue()).isBetween(70.0, 130.0);
        assertThat(f.daysLeft().doubleValue()).isBetween(5.0, 10.0);
        assertThat(f.confidence()).isEqualTo(Confidence.LOW);
    }

    @Test
    void recentHabitsWeighMoreThanOldOnes() {
        List<StockMovement> used = new ArrayList<>();
        for (int d = 0; d < 50; d++) used.add(outflow(NOW.minus(Duration.ofDays(d)), d < 10 ? "200" : "50"));

        var f = DepletionForecast.forecast(new BigDecimal("1000"), used, NOW, BOGOTA);

        // Plain average would be 80; the forecast leans to the last 10 days (200)
        assertThat(f.dailyRate().doubleValue()).isGreaterThan(100.0);
    }

    @Test
    void withoutConsumptionThereIsNoDepletionDate() {
        var f = DepletionForecast.forecast(new BigDecimal("5"), List.of(), NOW, BOGOTA);

        assertThat(f.dailyRate()).isEqualByComparingTo("0");
        assertThat(f.daysLeft()).isNull();
        assertThat(f.depletesAt()).isNull();
    }

    @Test
    void movementsOutsideTheWindowAreIgnored() {
        var f = DepletionForecast.forecast(new BigDecimal("5"), List.of(
                outflow(NOW.minus(Duration.ofDays(DepletionForecast.WINDOW_DAYS + 5)), "100"),
                outflow(NOW.plus(Duration.ofDays(2)), "100")), NOW, BOGOTA);

        assertThat(f.samples()).isZero();
        assertThat(f.daysLeft()).isNull();
    }

    private static StockMovement outflow(Instant at, String grams) {
        return new StockMovement(UUID.randomUUID(), null, null, MovementType.CONSUMPTION, new BigDecimal(grams).negate(),
                at, null, null, at);
    }
}
