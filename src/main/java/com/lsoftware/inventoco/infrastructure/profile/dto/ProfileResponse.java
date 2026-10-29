package com.lsoftware.inventoco.infrastructure.profile.dto;

import com.lsoftware.inventoco.entity.profile.model.UserProfile;

public record ProfileResponse(String email, String name, String country, String currency, String timeZone,
                              boolean sharePricesByDefault, boolean onboarded) {

    public static ProfileResponse from(UserProfile p) {
        return new ProfileResponse(p.email(), p.name(), p.country(), p.currency(), p.timeZone().getId(),
                p.sharePricesByDefault(), p.onboarded());
    }
}
