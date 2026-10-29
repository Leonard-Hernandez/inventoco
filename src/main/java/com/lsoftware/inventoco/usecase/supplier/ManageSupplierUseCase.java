package com.lsoftware.inventoco.usecase.supplier;

import com.lsoftware.inventoco.entity.exception.DomainException;
import com.lsoftware.inventoco.entity.exception.NotFoundException;
import com.lsoftware.inventoco.entity.profile.model.Countries;
import com.lsoftware.inventoco.entity.profile.model.UserProfile;
import com.lsoftware.inventoco.entity.supplier.gateway.SupplierGateway;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** The user's suppliers. System suppliers can be used by anyone but changed by no one. */
@Component
@RequiredArgsConstructor
public class ManageSupplierUseCase {

    private final SupplierGateway suppliers;
    private final Clock clock;

    /** country defaults to the user's. */
    public record SupplierCommand(String name, String country, String city, String url) {}

    @Transactional
    public Supplier create(UserProfile user, SupplierCommand c) {
        return suppliers.save(new Supplier(null, user.id(), name(c.name()), country(user, c.country()),
                blank(c.city()), blank(c.url()), false, clock.instant()));
    }

    @Transactional
    public Supplier update(UserProfile user, UUID id, SupplierCommand c) {
        return suppliers.save(owned(user.id(), id)
                .withDetails(name(c.name()), country(user, c.country()), blank(c.city()), blank(c.url())));
    }

    /** Archived suppliers keep their past prices but are no longer offered. */
    @Transactional
    public void archive(UserProfile user, UUID id) {
        suppliers.save(owned(user.id(), id).archive());
    }

    public List<Supplier> list(UserProfile user) {
        return suppliers.listAvailable(user.id(), user.country());
    }

    /** A supplier the user may record prices at: their own or a system one. */
    public Supplier usable(UUID userId, UUID id) {
        return suppliers.findById(id).filter(s -> s.usableBy(userId)).orElseThrow(ManageSupplierUseCase::notFound);
    }

    private Supplier owned(UUID userId, UUID id) {
        return suppliers.findById(id).filter(s -> !s.system() && s.ownerId().equals(userId) && !s.archived())
                .orElseThrow(ManageSupplierUseCase::notFound);
    }

    private static NotFoundException notFound() {
        return new NotFoundException("SUPPLIER_NOT_FOUND", "Supplier not found");
    }

    private static String country(UserProfile user, String country) {
        return country == null || country.isBlank() ? user.country() : Countries.country(country);
    }

    private static String name(String name) {
        if (name == null || name.isBlank()) throw new DomainException("INVALID_NAME", "The supplier needs a name");
        return name.strip();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
