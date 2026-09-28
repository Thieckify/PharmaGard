package com.pharmagarde.backend.infrastructure.persistence.repository;

import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Accès aux pharmacies en base de données. Les méthodes findBy... sont générées
 * automatiquement par Spring Data à partir de leur nom.
 */
public interface PharmacieRepository extends JpaRepository<PharmacieEntity, Long> {
    List<PharmacieEntity> findByZoneId(Long zoneId);
    List<PharmacieEntity> findByTenantUserId(Long tenantUserId);

    // Recherche publique : uniquement les pharmacies actives (RG-05), triées par nom
    List<PharmacieEntity> findByActiveTrueOrderByNomAsc();
    List<PharmacieEntity> findByQuartierIdAndActiveTrueOrderByNomAsc(Long quartierId);
}