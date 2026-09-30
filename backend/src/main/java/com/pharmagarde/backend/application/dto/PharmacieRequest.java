package com.pharmagarde.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Données envoyées par le pharmacien pour créer ou mettre à jour sa fiche pharmacie.
// La position (latitude/longitude) est obligatoire (RG-14) : sans elle, la pharmacie ne
// pourrait pas apparaître sur la carte du site.
public record PharmacieRequest(
        @NotBlank String nom,
        @NotBlank String adresse,
        @NotNull Long quartierId,
        @NotNull(message = "La latitude est obligatoire : placez la pharmacie sur la carte") Double latitude,
        @NotNull(message = "La longitude est obligatoire : placez la pharmacie sur la carte") Double longitude,
        String telephone,
        String horaireOuverture,
        String horaireFermeture,
        @NotBlank String numeroAgrementOrdre
) {
}