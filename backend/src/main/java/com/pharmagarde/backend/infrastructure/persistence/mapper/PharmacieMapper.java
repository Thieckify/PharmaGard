package com.pharmagarde.backend.infrastructure.persistence.mapper;

import com.pharmagarde.backend.domain.model.Pharmacie;
import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.QuartierEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.UserEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.ZoneEntity;

/**
 * Convertit entre PharmacieEntity (JPA, relations directes) et Pharmacie (domaine, Long simples).
 */
public class PharmacieMapper {

    private PharmacieMapper() {
        // classe utilitaire, pas d'instanciation
    }

    // Entité JPA -> modèle domaine
    public static Pharmacie toDomain(PharmacieEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Pharmacie(
                entity.getId(),
                entity.getNom(),
                entity.getAdresse(),
                entity.getQuartier() != null ? entity.getQuartier().getId() : null,
                entity.getZone() != null ? entity.getZone().getId() : null,
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getTelephone(),
                entity.getHoraireOuverture(),
                entity.getHoraireFermeture(),
                entity.isActive(),
                entity.getTenantUser() != null ? entity.getTenantUser().getId() : null,
                entity.getNumeroAgrementOrdre(),
                entity.getValideParUser() != null ? entity.getValideParUser().getId() : null,
                entity.getValideLe()
        );
    }

    // Modèle domaine -> entité JPA
    // quartier, zone, tenantUser et valideParUser doivent être résolus (chargés depuis leurs repositories)
    // par l'appelant avant conversion, car le domaine ne connaît que des Long (pas les entités liées).
    public static PharmacieEntity toEntity(Pharmacie domaine, QuartierEntity quartier, ZoneEntity zone,
                                            UserEntity tenantUser, UserEntity valideParUser) {
        if (domaine == null) {
            return null;
        }
        PharmacieEntity entity = new PharmacieEntity();
        entity.setId(domaine.getId());
        entity.setNom(domaine.getNom());
        entity.setAdresse(domaine.getAdresse());
        entity.setQuartier(quartier);
        entity.setZone(zone);
        entity.setLatitude(domaine.getLatitude());
        entity.setLongitude(domaine.getLongitude());
        entity.setTelephone(domaine.getTelephone());
        entity.setHoraireOuverture(domaine.getHoraireOuverture());
        entity.setHoraireFermeture(domaine.getHoraireFermeture());
        entity.setActive(domaine.isActive());
        entity.setTenantUser(tenantUser);
        entity.setNumeroAgrementOrdre(domaine.getNumeroAgrementOrdre());
        entity.setValideParUser(valideParUser);
        entity.setValideLe(domaine.getValideLe());
        return entity;
    }
}