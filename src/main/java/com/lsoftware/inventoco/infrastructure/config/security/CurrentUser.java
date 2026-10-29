package com.lsoftware.inventoco.infrastructure.config.security;

import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.usecase.profile.ResolveProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** The profile behind an access token (created on the first request of a new user). */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final ResolveProfileUseCase resolveProfile;

    public UserProfile of(Jwt jwt) {
        try {
            return resolveProfile.execute(jwt.getSubject(), jwt.getTokenValue());
        } catch (DataIntegrityViolationException e) {
            // A parallel first request created the profile in between: it exists now
            return resolveProfile.execute(jwt.getSubject(), jwt.getTokenValue());
        }
    }
}
