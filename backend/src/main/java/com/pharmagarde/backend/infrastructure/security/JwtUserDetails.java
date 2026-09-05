package com.pharmagarde.backend.infrastructure.security;

/**
 * Porte les informations de portée (scope) extraites du JWT : identité et rôle.
 * Utilisé comme principal de l'Authentication Spring Security de la requête en cours,
 * pour que les controllers y accèdent directement via @AuthenticationPrincipal.
 */
public record JwtUserDetails(Long userId, String email, Long tenantId, Long zoneId, String role) {
}