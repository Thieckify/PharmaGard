package com.pharmagarde.backend.infrastructure.persistence.mapper;

import com.pharmagarde.backend.domain.model.Garde;
import com.pharmagarde.backend.infrastructure.persistence.entity.GardeEntity;

/**
 * Convertit une GardeEntity (JPA) en modèle domaine Garde.
 * Pas de sens inverse (toEntity) pour l'instant : la création d'une GardeEntity
 * se fait directement dans GardeService, comme pour PharmacieService.
 */
public class GardeMapper {

    private GardeMapper() {
        // Classe utilitaire : que des méthodes statiques, jamais besoin d'un "new GardeMapper()"
    }

    public static Garde toDomain(GardeEntity entity) {
        return new Garde(
                entity.getId(),
                entity.getPharmacie().getId(),
                entity.getDateDebut(),
                entity.getDateFin(),
                entity.getType()
        );
    }
}