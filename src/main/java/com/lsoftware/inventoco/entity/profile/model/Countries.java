package com.lsoftware.inventoco.entity.profile.model;

import com.lsoftware.inventoco.entity.exception.DomainException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

/** ISO-3166 countries, their ISO-4217 currency and IANA time zones. */
public final class Countries {

    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());

    private Countries() {}

    public static String country(String code) {
        String c = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (!COUNTRIES.contains(c)) throw new DomainException("INVALID_COUNTRY", "Unknown country code: " + code);
        return c;
    }

    /** The currency used in a country (CO -> COP, ES -> EUR). */
    public static String currencyOf(String country) {
        Currency currency = Currency.getInstance(Locale.of("", country(country)));
        if (currency == null) throw new DomainException("INVALID_COUNTRY", "No currency for country " + country);
        return currency.getCurrencyCode();
    }

    public static String currency(String code) {
        try {
            return Currency.getInstance(code == null ? "" : code.strip().toUpperCase(Locale.ROOT)).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_CURRENCY", "Unknown currency code: " + code);
        }
    }

    public static ZoneId timeZone(String id) {
        try {
            return ZoneId.of(id == null ? "" : id.strip());
        } catch (DateTimeException e) {
            throw new DomainException("INVALID_TIME_ZONE", "Unknown time zone: " + id);
        }
    }
}
