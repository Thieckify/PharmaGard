package com.pharmagarde.backend.infrastructure.persistence.repository;

import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PharmacieRepository extends JpaRepository<PharmacieEntity, Long> {
    List<PharmacieEntity> findByZoneId(Long zoneId);
    List<PharmacieEntity> findByTenantUserId(Long tenantUserId);
}