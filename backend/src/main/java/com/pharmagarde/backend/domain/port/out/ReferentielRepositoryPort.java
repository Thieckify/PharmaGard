package com.pharmagarde.backend.domain.port.out;

import com.pharmagarde.backend.domain.model.Quartier;
import com.pharmagarde.backend.domain.model.Zone;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie pour le référentiel géographique : les 11 zones de garde et leurs quartiers.
 */
public interface ReferentielRepositoryPort {

    List<Zone> listerZones();

    List<Quartier> listerQuartiers();

    Optional<Quartier> trouverQuartier(Long id);

    boolean quartierExiste(Long id);
}
