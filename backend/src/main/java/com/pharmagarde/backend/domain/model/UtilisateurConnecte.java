package com.pharmagarde.backend.domain.model;

import com.pharmagarde.backend.domain.enums.Role;

/**
 * Identité et périmètre de l'utilisateur qui fait la requête, tels qu'inscrits dans son jeton.
 * Objet du domaine pur : les services raisonnent sur lui sans rien savoir de JWT ni de Spring
 * Security. C'est le contrôleur qui le construit à partir du jeton reçu.
 */
public record UtilisateurConnecte(Long userId, Role role, Long tenantId, Long zoneId) {

    public boolean estPharmacien() {
        return role == Role.PHARMACIEN;
    }

    // Superviseur rattaché à une zone précise (le superviseur général a une zone vide)
    public boolean estSuperviseurDeZone() {
        return role == Role.SUPERVISEUR && zoneId != null;
    }
}
