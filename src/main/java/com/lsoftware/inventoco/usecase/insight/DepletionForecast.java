package com.lsoftware.inventoco.usecase.insight;

import com.lsoftware.inventoco.entity.item.model.StockMovement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * When an item will run out, from its recorded consumption. Outflows (consumption and waste) are
 * grouped into days in the user's time zone, and the daily rate is a weighted average where each
 * day counts half as much every HALF_LIFE_DAYS: recent habits matter more than old ones, while
 * sparse records (a weekly "used 1 kg") still average out over the days in between.
 */
public final class DepletionForecast {

    public static final int WINDOW_DAYS = 60;
    static final double HALF_LIFE_DAYS = 14;

    public enum Confidence { LOW, MEDIUM, HIGH }

    /**
     * @param dailyRate      base units per day (0 = no consumption recorded)
     * @param daysLeft       null when nothing is being consumed
     * @param depletesAt     null when nothing is being consumed
     * @param samples        outflow movements used
     */
    public record Result(BigDecimal dailyRate, BigDecimal daysLeft, Instant depletesAt, Confidence confidence,
                         int samples) {}

    private DepletionForecast() {}

    /** @param outflows this item's outflows in the window, any order (quantities are negative) */
    public static Result forecast(BigDecimal currentStock, List<StockMovement> outflows, Instant now, ZoneId zone) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        LocalDate windowStart = today.minusDays(WINDOW_DAYS - 1);
        double[] perDay = new double[WINDOW_DAYS];
        LocalDate first = null;
        int samples = 0;
        for (StockMovement m : outflows) {
            LocalDate day = LocalDate.ofInstant(m.occurredAt(), zone);
            if (day.isBefore(windowStart) || day.isAfter(today)) continue;
            perDay[(int) ChronoUnit.DAYS.between(windowStart, day)] += m.quantity().abs().doubleValue();
            if (first == null || day.isBefore(first)) first = day;
            samples++;
        }
        if (samples == 0) return new Result(BigDecimal.ZERO, null, null, Confidence.LOW, 0);

        // Weighted average over the days since consumption started being recorded (today included)
        double weighted = 0;
        double weights = 0;
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            double w = Math.pow(0.5, ChronoUnit.DAYS.between(d, today) / HALF_LIFE_DAYS);
            weighted += w * perDay[(int) ChronoUnit.DAYS.between(windowStart, d)];
            weights += w;
        }
        double rate = weighted / weights;
        long spanDays = ChronoUnit.DAYS.between(first, today) + 1;
        BigDecimal daily = BigDecimal.valueOf(rate).setScale(6, RoundingMode.HALF_UP);
        if (daily.signum() == 0) return new Result(BigDecimal.ZERO, null, null, confidence(samples, spanDays), samples);

        double days = currentStock.doubleValue() / rate;
        Instant depletesAt = now.plusSeconds(Math.round(days * 86_400));
        return new Result(daily, BigDecimal.valueOf(days).setScale(1, RoundingMode.HALF_UP), depletesAt,
                confidence(samples, spanDays), samples);
    }

    static Confidence confidence(int samples, long spanDays) {
        if (samples < 5 || spanDays < 7) return Confidence.LOW;
        if (samples < 15 || spanDays < 21) return Confidence.MEDIUM;
        return Confidence.HIGH;
    }
}
