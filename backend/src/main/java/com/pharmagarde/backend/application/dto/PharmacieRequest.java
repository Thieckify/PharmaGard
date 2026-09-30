package com.pharmagarde.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Données envoyées par le pharmacien pour créer ou mettre à jour sa fiche pharmacie.
// La position (latitude/longitude) est obligatoire (RG-14) : sans elle, la pharmacie ne
// pourrait pas apparaître sur la carte du site.
// Le numéro d'agrément n'est pas validé ici : il est exigé à la création (vérifié dans
// PharmacieService). À la mise à jour, un numéro vide ou identique ne change rien, et un
// numéro différent remet la fiche en attente de validation (RG-16).
public record PharmacieRequest(
        @NotBlank String nom,
        @NotBlank String adresse,
        @NotNull Long quartierId,
        @NotNull(message = "La latitude est obligatoire : placez la pharmacie sur la carte") Double latitude,
        @NotNull(message = "La longitude est obligatoire : placez la pharmacie sur la carte") Double longitude,
        String telephone,
        String horaireOuverture,
        String horaireFermeture,
        String numeroAgrementOrdre
) {
}