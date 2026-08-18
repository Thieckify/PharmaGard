package com.pharmagarde.backend.application.dto;

/**
 * Réponse renvoyée après une connexion réussie.
 * Contient le token JWT ainsi que les infos de base de l'utilisateur,
 * pratiques pour que le frontend Angular affiche l'UI adaptée au rôle
 * sans avoir à redécoder le token côté client.
 */
public record LoginResponse(
                String token,
                Long userId,
                String email,
                String role,
                Long tenantId,
                Long zoneId) {
}