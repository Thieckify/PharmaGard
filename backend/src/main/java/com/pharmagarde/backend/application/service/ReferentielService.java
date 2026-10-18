package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.QuartierResponse;
import com.pharmagarde.backend.application.support.LibellesReferentiel;
import com.pharmagarde.backend.domain.port.out.ReferentielRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Expose les données de référentiel (zones, quartiers) utilisées par les formulaires
 * et la recherche publique. Données en lecture seule.
 */
@Service
public class ReferentielService {

    private final ReferentielRepositoryPort referentiel;

    public ReferentielService(ReferentielRepositoryPort referentiel) {
        this.referentiel = referentiel;
    }

    // Liste tous les quartiers avec le nom de leur zone et leur centre approximatif
    // (pour peupler un menu déroulant et recentrer les cartes)
    public List<QuartierResponse> listQuartiers() {
        LibellesReferentiel libelles = LibellesReferentiel.charger(referentiel);
        return referentiel.listerQuartiers().stream()
                .map(q -> new QuartierResponse(
                        q.getId(), q.getNom(),
                        q.getZoneId(), libelles.nomZone(q.getZoneId()),
                        q.getLatitude(), q.getLongitude()))
                .toList();
    }
}
