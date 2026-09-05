package com.pharmagarde.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Données envoyées par le pharmacien pour créer ou mettre à jour sa fiche pharmacie
public record PharmacieRequest(
        @NotBlank String nom,
        @NotBlank String adresse,
        @NotNull Long quartierId,
        Double latitude,
        Double longitude,
        String telephone,
        String horaireOuverture,
        String horaireFermeture,
        @NotBlank String numeroAgrementOrdre
) {
}