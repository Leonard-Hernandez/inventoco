package com.lsoftware.inventoco.infrastructure.mcp;

import com.lsoftware.inventoco.entity.exception.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/** Typed access to the arguments of a tool call. */
public record McpArgs(Map<String, Object> values) {

    public String string(String name) {
        Object v = values.get(name);
        return v == null || v.toString().isBlank() ? null : v.toString().strip();
    }

    public String requiredString(String name) {
        String v = string(name);
        if (v == null) throw missing(name);
        return v;
    }

    public UUID id(String name) {
        String v = string(name);
        if (v == null) return null;
        try {
            return UUID.fromString(v);
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_ARGUMENT", name + " must be an id returned by another tool");
        }
    }

    public UUID requiredId(String name) {
        UUID v = id(name);
        if (v == null) throw missing(name);
        return v;
    }

    public BigDecimal decimal(String name) {
        Object v = values.get(name);
        if (v == null || v.toString().isBlank()) return null;
        try {
            return new BigDecimal(v.toString().strip());
        } catch (NumberFormatException e) {
            throw new DomainException("INVALID_ARGUMENT", name + " must be a number");
        }
    }

    public BigDecimal requiredDecimal(String name) {
        BigDecimal v = decimal(name);
        if (v == null) throw missing(name);
        return v;
    }

    public Integer integer(String name) {
        BigDecimal v = decimal(name);
        return v == null ? null : v.intValue();
    }

    public boolean bool(String name, boolean fallback) {
        Object v = values.get(name);
        if (v == null) return fallback;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString().strip());
    }

    /**
     * An instant: ISO with offset (2026-10-21T09:30:00-05:00), or a plain date / local date-time
     * read in the user's time zone. Null when absent (= now).
     */
    public Instant instant(String name, ZoneId zone) {
        String v = string(name);
        if (v == null) return null;
        try {
            if (v.length() == 10) return LocalDate.parse(v).atTime(12, 0).atZone(zone).toInstant();
            if (v.endsWith("Z") || v.matches(".*[+-]\\d\\d:\\d\\d$")) return java.time.OffsetDateTime.parse(v).toInstant();
            return java.time.LocalDateTime.parse(v).atZone(zone).toInstant();
        } catch (DateTimeParseException e) {
            throw new DomainException("INVALID_ARGUMENT", name + " must be a date like 2026-10-21 or 2026-10-21T09:30");
        }
    }

    private static DomainException missing(String name) {
        return new DomainException("INVALID_ARGUMENT", "Missing argument: " + name);
    }
}
