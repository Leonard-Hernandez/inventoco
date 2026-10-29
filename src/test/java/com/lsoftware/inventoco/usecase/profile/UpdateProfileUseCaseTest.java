package com.lsoftware.inventoco.usecase.profile;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.profile.gateway.ProfileGateway;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import org.junit.jupiter.api.Test;

import static com.lsoftware.inventoco.usecase.Fixtures.ANA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateProfileUseCaseTest {

    private final ProfileGateway profiles = mock(ProfileGateway.class);
    private final UpdateProfileUseCase useCase = new UpdateProfileUseCase(profiles);

    @Test
    void theCurrencyDefaultsToTheCountrysOne() {
        when(profiles.save(any())).thenAnswer(i -> i.getArgument(0));

        UserProfile mx = useCase.execute(ANA, "mx", null, "America/Mexico_City", true);
        UserProfile es = useCase.execute(ANA, "ES", "", "Europe/Madrid", false);

        assertThat(mx.currency()).isEqualTo("MXN");
        assertThat(mx.country()).isEqualTo("MX");
        assertThat(mx.timeZone().getId()).isEqualTo("America/Mexico_City");
        assertThat(mx.onboarded()).isTrue();
        assertThat(es.currency()).isEqualTo("EUR");
    }

    @Test
    void anExplicitCurrencyWins() {
        when(profiles.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(useCase.execute(ANA, "EC", "usd", "America/Guayaquil", false).currency()).isEqualTo("USD");
    }

    @Test
    void unknownCodesAreRejected() {
        assertThatThrownBy(() -> useCase.execute(ANA, "XX", null, "UTC", false)).isInstanceOf(DomainException.class)
                .hasMessageContaining("Unknown country");
        assertThatThrownBy(() -> useCase.execute(ANA, "CO", "ABC", "UTC", false)).hasMessageContaining("Unknown currency");
        assertThatThrownBy(() -> useCase.execute(ANA, "CO", null, "Mars/Olympus", false)).hasMessageContaining("Unknown time zone");
    }
}
