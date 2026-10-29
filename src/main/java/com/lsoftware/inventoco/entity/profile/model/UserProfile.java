package com.lsoftware.inventoco.entity.profile.model;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * One user of the app, linked to an authco subject. Country, currency and time zone decide how
 * prices are compared (same currency only) and how instants are shown and grouped into days.
 */
public record UserProfile(
        UUID id,
        String authcoSub,
        String email,
        String name,
        String country,
        String currency,
        ZoneId timeZone,
        boolean sharePricesByDefault,
        /** False until the user confirmed country, currency and time zone. */
        boolean onboarded,
        Instant createdAt
) {

    public static final String DEFAULT_COUNTRY = "US";
    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("UTC");

    public static UserProfile firstVisit(String sub, String email, String name, Instant now) {
        return new UserProfile(null, sub, email, name, DEFAULT_COUNTRY, Countries.currencyOf(DEFAULT_COUNTRY),
                DEFAULT_TIME_ZONE, false, false, now);
    }

    public UserProfile withSettings(String country, String currency, ZoneId timeZone, boolean sharePricesByDefault) {
        return new UserProfile(id, authcoSub, email, name, country, currency, timeZone, sharePricesByDefault, true, createdAt);
    }
}
