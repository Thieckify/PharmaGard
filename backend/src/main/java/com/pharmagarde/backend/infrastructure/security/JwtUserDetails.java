package com.pharmagarde.backend.infrastructure.security;

import com.pharmagarde.backend.domain.enums.Role;
import com.pharmagarde.backend.domain.model.UtilisateurConnecte;

/**
 * Identité de l'utilisateur extraite du JWT, posée comme principal Spring Security par
 * JwtAuthFilter. Les contrôleurs la convertissent en UtilisateurConnecte, l'objet du domaine
 * que manipulent les services : la couche application ne connaît jamais ce type.
 */
public record JwtUserDetails(Long userId, String email, Long tenantId, Long zoneId, String role) {

    public UtilisateurConnecte versUtilisateurConnecte() {
        return new UtilisateurConnecte(userId, Role.valueOf(role), tenantId, zoneId);
    }
}
