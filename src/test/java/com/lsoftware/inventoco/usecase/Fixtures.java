package com.lsoftware.inventoco.usecase;

import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

public final class Fixtures {

    public static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    public static final UserProfile ANA = profile("sub-ana");
    public static final UserProfile LUIS = profile("sub-luis");

    private Fixtures() {}

    public static UserProfile profile(String sub) {
        return new UserProfile(UUID.nameUUIDFromBytes(sub.getBytes()), sub, sub + "@example.com", sub, "CO", "COP",
                BOGOTA, false, true, NOW.minusSeconds(86_400 * 365));
    }

    /** Rice in kg: stock and minimum given in kg. */
    public static Item rice(UserProfile owner, String stockKg, String minKg, UUID catalogProductId) {
        return new Item(UUID.nameUUIDFromBytes(("rice-" + owner.authcoSub()).getBytes()), owner.id(), "Arroz", "Despensa",
                Unit.KG.dimension(), Unit.KG, Unit.KG.toBase(new BigDecimal(minKg)), Unit.KG.toBase(new BigDecimal(stockKg)),
                null, catalogProductId, false, NOW, NOW);
    }

    public static Supplier supplier(UserProfile owner, String name) {
        return new Supplier(UUID.nameUUIDFromBytes((name + (owner == null ? "" : owner.authcoSub())).getBytes()),
                owner == null ? null : owner.id(), name, "CO", "Bogotá", null, false, NOW);
    }

    public static BigDecimal kg(String value) {
        return Unit.KG.toBase(new BigDecimal(value));
    }
}
