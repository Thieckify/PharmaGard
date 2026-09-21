package com.pharmagarde.backend.application.dto;

import com.pharmagarde.backend.domain.enums.TypeGarde;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de requête pour la déclaration d'une période de garde par un pharmacien.
 * La pharmacie n'est pas précisée ici : elle est déduite du compte connecté,
 * même principe que PharmacieRequest pour l'upsert de fiche.
 */
public record GardeRequest(
        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        @NotNull(message = "Le type de garde est obligatoire")
        TypeGarde type
) {
}