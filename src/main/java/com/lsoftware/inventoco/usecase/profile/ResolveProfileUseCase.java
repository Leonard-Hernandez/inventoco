package com.lsoftware.inventoco.usecase.profile;

import com.lsoftware.inventoco.entity.identity.gateway.IdentityGateway;
import com.lsoftware.inventoco.entity.identity.model.UserIdentity;
import com.lsoftware.inventoco.entity.profile.gateway.ProfileGateway;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * The profile of the caller. Anyone with an authco account can use the app, so the first
 * request creates the profile (email and name come from userinfo, since tokens only carry sub).
 */
@Component
@RequiredArgsConstructor
public class ResolveProfileUseCase {

    private final ProfileGateway profiles;
    private final IdentityGateway identity;
    private final Clock clock;

    @Transactional
    public UserProfile execute(String sub, String accessToken) {
        return profiles.findBySub(sub).orElseGet(() -> {
            UserIdentity user = identity.userInfo(accessToken);
            return profiles.save(UserProfile.firstVisit(sub, user.email(), user.name(), clock.instant()));
        });
    }
}
