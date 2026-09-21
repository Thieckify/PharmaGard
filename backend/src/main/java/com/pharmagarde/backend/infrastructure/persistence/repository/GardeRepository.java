package com.pharmagarde.backend.infrastructure.persistence.repository;

import com.pharmagarde.backend.infrastructure.persistence.entity.GardeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Accès aux périodes de garde en base.
 */
public interface GardeRepository extends JpaRepository<GardeEntity, Long> {

    // Toutes les périodes déjà déclarées par une pharmacie — utilisé pour
    // vérifier le chevauchement (RG-03) avant d'en enregistrer une nouvelle.
    List<GardeEntity> findByPharmacieId(Long pharmacieId);

    // Périodes actives à une date précise (date1 == date2 == la date recherchée) —
    // ne sert pas encore, prévu pour l'endpoint public GET /api/public/gardes?date=...
    List<GardeEntity> findByDateDebutLessThanEqualAndDateFinGreaterThanEqual(LocalDate date1, LocalDate date2);
}