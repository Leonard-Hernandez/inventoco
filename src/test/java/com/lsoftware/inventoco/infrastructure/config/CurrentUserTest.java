package com.lsoftware.inventoco.infrastructure.config;

import com.lsoftware.inventoco.infrastructure.config.security.CurrentUser;
import com.lsoftware.inventoco.usecase.profile.ResolveProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static com.lsoftware.inventoco.usecase.Fixtures.ANA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CurrentUserTest {

    @Test
    void aParallelFirstVisitReadsTheProfileTheOtherRequestCreated() {
        ResolveProfileUseCase resolve = mock(ResolveProfileUseCase.class);
        when(resolve.execute("sub-ana", "token"))
                .thenThrow(new DataIntegrityViolationException("duplicate authco_sub"))
                .thenReturn(ANA);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-ana")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

        assertThat(new CurrentUser(resolve).of(jwt)).isEqualTo(ANA);
        verify(resolve, times(2)).execute("sub-ana", "token");
    }
}
