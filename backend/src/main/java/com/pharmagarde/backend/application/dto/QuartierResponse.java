package com.pharmagarde.backend.application.dto;

/**
 * Représentation d'un quartier renvoyée au frontend (référentiel public).
 */
public record QuartierResponse(
        Long id,
        String nom,
        Long zoneId,
        String nomZone
) {
}