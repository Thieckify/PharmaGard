package com.pharmagarde.backend.infrastructure.persistence.mapper;

import com.pharmagarde.backend.domain.model.User;
import com.pharmagarde.backend.infrastructure.persistence.entity.UserEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.ZoneEntity;

/**
 * Convertit un utilisateur entre sa forme base de données (UserEntity)
 * et sa forme métier (User), pour que la couche application ne manipule
 * jamais directement une entité JPA. Classe utilitaire statique, comme GardeMapper.
 */
public final class UserMapper {

    private UserMapper() {
    }

    // Entité -> modèle métier : l'objet zone devient un simple zoneId (ou null)
    public static User toDomain(UserEntity entity) {
        Long zoneId = entity.getZone() != null ? entity.getZone().getId() : null;
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getRole(),
                entity.getTenantId(),
                zoneId,
                entity.isActive()
        );
    }

    // Modèle métier -> entité : la zone est fournie déjà résolue par l'appelant (null si aucune)
    public static UserEntity toEntity(User user, ZoneEntity zone) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());
        entity.setTenantId(user.getTenantId());
        entity.setZone(zone);
        entity.setActive(user.isActive());
        return entity;
    }
}