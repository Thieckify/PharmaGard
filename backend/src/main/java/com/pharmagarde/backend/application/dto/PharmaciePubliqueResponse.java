package com.pharmagarde.backend.application.dto;

/**
 * Représentation publique d'une pharmacie dans les résultats de recherche.
 * Comme PharmacieDeGardeResponse, elle est volontairement séparée de PharmacieResponse :
 * aucune information interne (tenant, validation) ne doit sortir sur un endpoint
 * accessible sans authentification (RG-04).
 */
public record PharmaciePubliqueResponse(
        Long pharmacieId,
        String nom,
        String adresse,
        String nomQuartier,
        String nomZone,
        Double latitude,
        Double longitude,
        String telephone,
        String horaireOuverture,
        String horaireFermeture
) {
}