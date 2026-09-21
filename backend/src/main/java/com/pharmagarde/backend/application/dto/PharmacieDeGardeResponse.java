package com.pharmagarde.backend.application.dto;

import com.pharmagarde.backend.domain.enums.TypeGarde;

import java.time.LocalDate;

/**
 * Représentation publique d'une pharmacie de garde à une date donnée.
 * Volontairement séparé de PharmacieResponse : ne contient AUCUNE information
 * administrative interne (validation, tenant), puisque cet endpoint est accessible
 * sans authentification (RG-04).
 */
public record PharmacieDeGardeResponse(
        Long pharmacieId,
        String nom,
        String adresse,
        String nomQuartier,
        String nomZone,
        Double latitude,
        Double longitude,
        String telephone,
        String horaireOuverture,
        String horaireFermeture,
        TypeGarde typeGarde,
        LocalDate dateDebutGarde,
        LocalDate dateFinGarde
) {
}