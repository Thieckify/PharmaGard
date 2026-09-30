package com.pharmagarde.backend.application.dto;

import java.time.LocalDateTime;

// Représentation d'une pharmacie renvoyée au frontend (routes authentifiées uniquement).
// numeroAgrementOrdre permet au superviseur de contrôler la fiche au moment de la valider.
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
        Long tenantUserId,
        Long valideParUserId,
        LocalDateTime valideLe,
        String numeroAgrementOrdre) {
}