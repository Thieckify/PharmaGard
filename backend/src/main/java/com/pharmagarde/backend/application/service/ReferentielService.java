package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.QuartierResponse;
import com.pharmagarde.backend.infrastructure.persistence.repository.QuartierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Expose les données de référentiel (zones, quartiers) utilisées par les formulaires
 * et la recherche publique. Données en lecture seule pour l'instant.
 */
@Service
public class ReferentielService {

    private final QuartierRepository quartierRepository;

    public ReferentielService(QuartierRepository quartierRepository) {
        this.quartierRepository = quartierRepository;
    }

    // Liste tous les quartiers avec le nom de leur zone (pour peupler un menu déroulant)
    public List<QuartierResponse> listQuartiers() {
        return quartierRepository.findAll().stream()
                .map(q -> new QuartierResponse(q.getId(), q.getNom(), q.getZone().getId(), q.getZone().getNom()))
                .toList();
    }
}