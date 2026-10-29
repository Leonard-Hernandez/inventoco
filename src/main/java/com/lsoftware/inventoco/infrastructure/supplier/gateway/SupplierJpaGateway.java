package com.lsoftware.inventoco.infrastructure.supplier.gateway;

import com.lsoftware.inventoco.entity.supplier.gateway.SupplierGateway;
import com.lsoftware.inventoco.entity.supplier.model.Supplier;
import com.lsoftware.inventoco.infrastructure.config.db.repository.JpaSupplierRepository;
import com.lsoftware.inventoco.infrastructure.config.db.schema.SupplierEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupplierJpaGateway implements SupplierGateway {

    private final JpaSupplierRepository repo;

    @Override
    public Optional<Supplier> findById(UUID id) {
        return repo.findById(id).map(SupplierJpaGateway::toDomain);
    }

    @Override
    public List<Supplier> findAllById(Collection<UUID> ids) {
        return repo.findAllById(ids).stream().map(SupplierJpaGateway::toDomain).toList();
    }

    @Override
    public List<Supplier> listAvailable(UUID ownerId, String country) {
        return repo.listAvailable(ownerId, country).stream().map(SupplierJpaGateway::toDomain).toList();
    }

    @Override
    public Supplier save(Supplier s) {
        return toDomain(repo.save(SupplierEntity.builder()
                .id(s.id() != null ? s.id() : UUID.randomUUID())
                .ownerId(s.ownerId()).name(s.name()).country(s.country()).city(s.city()).url(s.url())
                .archived(s.archived()).createdAt(s.createdAt())
                .build()));
    }

    static Supplier toDomain(SupplierEntity e) {
        return new Supplier(e.getId(), e.getOwnerId(), e.getName(), e.getCountry(), e.getCity(), e.getUrl(),
                e.isArchived(), e.getCreatedAt());
    }
}
