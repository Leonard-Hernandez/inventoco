package com.lsoftware.inventoco.infrastructure.profile.gateway;

import com.lsoftware.inventoco.entity.profile.gateway.ProfileGateway;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.infrastructure.config.db.repository.JpaUserProfileRepository;
import com.lsoftware.inventoco.infrastructure.config.db.schema.UserProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileJpaGateway implements ProfileGateway {

    private final JpaUserProfileRepository repo;

    @Override
    public Optional<UserProfile> findBySub(String authcoSub) {
        return repo.findByAuthcoSub(authcoSub).map(ProfileJpaGateway::toDomain);
    }

    @Override
    public UserProfile save(UserProfile p) {
        return toDomain(repo.save(UserProfileEntity.builder()
                .id(p.id() != null ? p.id() : UUID.randomUUID())
                .authcoSub(p.authcoSub()).email(p.email()).name(p.name())
                .country(p.country()).currency(p.currency()).timeZone(p.timeZone().getId())
                .sharePricesByDefault(p.sharePricesByDefault()).onboarded(p.onboarded())
                .createdAt(p.createdAt())
                .build()));
    }

    static UserProfile toDomain(UserProfileEntity e) {
        return new UserProfile(e.getId(), e.getAuthcoSub(), e.getEmail(), e.getName(), e.getCountry(), e.getCurrency(),
                ZoneId.of(e.getTimeZone()), e.isSharePricesByDefault(), e.isOnboarded(), e.getCreatedAt());
    }
}
