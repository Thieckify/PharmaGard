package com.pharmagarde.backend.infrastructure.security;

/**
 * Porte les informations de portée (scope) extraites du JWT,
 * attachées à l'authentification Spring Security de la requête en cours.
 * Permet aux controllers d'accéder au tenantId/zoneId sans redécoder le token.
 */
public record JwtUserDetails(Long userId, Long tenantId, Long zoneId) {
}