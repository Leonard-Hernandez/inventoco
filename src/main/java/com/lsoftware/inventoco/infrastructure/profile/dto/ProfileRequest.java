package com.lsoftware.inventoco.infrastructure.profile.dto;

import jakarta.validation.constraints.NotBlank;

/** currency is optional: it defaults to the country's. */
public record ProfileRequest(@NotBlank String country, String currency, @NotBlank String timeZone,
                             boolean sharePricesByDefault) {}
