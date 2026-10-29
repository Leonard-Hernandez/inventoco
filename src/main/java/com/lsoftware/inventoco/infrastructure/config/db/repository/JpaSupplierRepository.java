package com.lsoftware.inventoco.infrastructure.config.db.repository;

import com.lsoftware.inventoco.infrastructure.config.db.schema.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaSupplierRepository extends JpaRepository<SupplierEntity, UUID> {

    @Query("""
            select s from SupplierEntity s
            where s.archived = false and (s.ownerId = :owner or (s.ownerId is null and s.country = :country))
            order by case when s.ownerId is null then 1 else 0 end, s.name""")
    List<SupplierEntity> listAvailable(@Param("owner") UUID owner, @Param("country") String country);

    @Query("select s from SupplierEntity s where s.ownerId is null and s.country = :country and lower(s.name) = lower(:name)")
    Optional<SupplierEntity> findSystem(@Param("country") String country, @Param("name") String name);
}
