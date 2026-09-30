package com.pharmagarde.backend.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de la requête d'inscription self-service (POST /api/auth/register).
 * Contient les identifiants du compte ET la fiche de la pharmacie : un pharmacien
 * s'inscrit toujours avec sa pharmacie, tout est créé ensemble ou rien.
 * Le rôle n'apparaît jamais ici : il est forcé côté service (PHARMACIEN),
 * pour qu'un client ne puisse jamais s'auto-attribuer un rôle plus élevé.
 */
public record RegisterRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être valide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,

        // @Valid : fait aussi vérifier les contraintes internes de PharmacieRequest
        // (nom, adresse, quartierId, numéro d'agrément obligatoires)
        @NotNull(message = "Les informations de la pharmacie sont obligatoires")
        @Valid
        PharmacieRequest pharmacie
) {
}