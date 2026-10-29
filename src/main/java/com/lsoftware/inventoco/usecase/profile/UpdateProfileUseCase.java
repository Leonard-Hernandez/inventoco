package com.lsoftware.inventoco.usecase.profile;

import com.lsoftware.inventoco.entity.profile.gateway.ProfileGateway;
import com.lsoftware.inventoco.entity.profile.model.Countries;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Country, currency (defaults to the country's) and time zone of the user. */
@Component
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final ProfileGateway profiles;

    @Transactional
    public UserProfile execute(UserProfile current, String country, String currency, String timeZone,
                               boolean sharePricesByDefault) {
        String c = Countries.country(country);
        String cur = currency == null || currency.isBlank() ? Countries.currencyOf(c) : Countries.currency(currency);
        return profiles.save(current.withSettings(c, cur, Countries.timeZone(timeZone), sharePricesByDefault));
    }
}
