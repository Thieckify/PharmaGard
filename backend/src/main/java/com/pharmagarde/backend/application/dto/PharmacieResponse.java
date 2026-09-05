package com.pharmagarde.backend.application.dto;

// Représentation d'une pharmacie renvoyée au frontend
public record PharmacieResponse(
        Long id,
        String nom,
        String adresse,
        Long quartierId,
        String nomQuartier,
        Long zoneId,
        String nomZone,
        Double latitude,
        Double longitude,
        String telephone,
        String horaireOuverture,
        String horaireFermeture,
        boolean active,
        Long tenantUserId) {
}