package com.lsoftware.inventoco.entity.profile.gateway;

import com.lsoftware.inventoco.entity.profile.model.UserProfile;

import java.util.Optional;

public interface ProfileGateway {
    Optional<UserProfile> findBySub(String authcoSub);
    UserProfile save(UserProfile profile);
}
