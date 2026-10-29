package com.lsoftware.inventoco.infrastructure.mcp;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.lsoftware.inventoco.usecase.Fixtures.BOGOTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpArgsTest {

    @Test
    void datesWithoutOffsetAreReadInTheUsersTimeZone() {
        var a = new McpArgs(Map.of("local", "2026-10-21T19:00", "offset", "2026-10-21T19:00:00Z", "day", "2026-10-21"));

        assertThat(a.instant("local", BOGOTA)).isEqualTo(Instant.parse("2026-10-22T00:00:00Z"));
        assertThat(a.instant("offset", BOGOTA)).isEqualTo(Instant.parse("2026-10-21T19:00:00Z"));
        assertThat(a.instant("day", BOGOTA)).isEqualTo(Instant.parse("2026-10-21T17:00:00Z"));
        assertThat(a.instant("missing", BOGOTA)).isNull();
    }

    @Test
    void badValuesExplainWhatIsExpected() {
        var a = new McpArgs(Map.of("id", "7", "n", "abc"));

        assertThatThrownBy(() -> a.requiredId("id")).hasMessageContaining("id returned by another tool");
        assertThatThrownBy(() -> a.decimal("n")).hasMessageContaining("must be a number");
    }
}
