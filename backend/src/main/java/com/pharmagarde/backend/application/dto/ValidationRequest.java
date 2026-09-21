package com.pharmagarde.backend.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour la validation ou le refus d'une pharmacie par un superviseur.
 * "valide" pilote le sens de l'action : true = validation, false = refus (dévalidation).
 */
public record ValidationRequest(
        @NotNull(message = "Le champ 'valide' est obligatoire")
        Boolean valide
) {
}