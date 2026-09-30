package com.pharmagarde.backend.application.dto;

/**
 * Représentation d'un quartier renvoyée au frontend (référentiel public).
 * latitude/longitude sont le centre approximatif du quartier (null si inconnu) : le frontend
 * s'en sert pour ouvrir les cartes sur le bon endroit.
 */
public record QuartierResponse(
        Long id,
        String nom,
        Long zoneId,
        String nomZone,
        Double latitude,
        Double longitude
) {
}