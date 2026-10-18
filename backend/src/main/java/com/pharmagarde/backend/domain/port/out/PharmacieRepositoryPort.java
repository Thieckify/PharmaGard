package com.pharmagarde.backend.domain.port.out;

import com.pharmagarde.backend.domain.model.Pharmacie;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port de sortie : ce dont la couche application a besoin pour lire et enregistrer les fiches
 * pharmacies. Elle ne sait pas qui l'implémente (aujourd'hui JPA et PostgreSQL).
 */
public interface PharmacieRepositoryPort {

    Optional<Pharmacie> trouverParId(Long id);

    List<Pharmacie> trouverParIds(Collection<Long> ids);

    // Un pharmacien possède au plus une pharmacie : il en est le tenant
    Optional<Pharmacie> trouverParTenant(Long tenantUserId);

    List<Pharmacie> listerToutes();

    List<Pharmacie> listerParZone(Long zoneId);

    // Pharmacies actives (validées), triées par nom : seules visibles du public (RG-05)
    List<Pharmacie> listerActives();

    List<Pharmacie> listerActivesParQuartier(Long quartierId);

    Pharmacie enregistrer(Pharmacie pharmacie);

    void supprimer(Long id);
}
