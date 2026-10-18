package com.pharmagarde.backend.infrastructure.persistence.adapter;

import com.pharmagarde.backend.domain.model.Quartier;
import com.pharmagarde.backend.domain.model.Zone;
import com.pharmagarde.backend.domain.port.out.ReferentielRepositoryPort;
import com.pharmagarde.backend.infrastructure.persistence.entity.QuartierEntity;
import com.pharmagarde.backend.infrastructure.persistence.repository.QuartierRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.ZoneRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port ReferentielRepositoryPort (zones et quartiers, en lecture seule).
 */
@Component
public class ReferentielRepositoryAdapter implements ReferentielRepositoryPort {

    private final ZoneRepository zoneRepository;
    private final QuartierRepository quartierRepository;

    public ReferentielRepositoryAdapter(ZoneRepository zoneRepository, QuartierRepository quartierRepository) {
        this.zoneRepository = zoneRepository;
        this.quartierRepository = quartierRepository;
    }

    @Override
    public List<Zone> listerZones() {
        return zoneRepository.findAll().stream().map(z -> new Zone(z.getId(), z.getNom())).toList();
    }

    @Override
    public List<Quartier> listerQuartiers() {
        return quartierRepository.findAll().stream().map(ReferentielRepositoryAdapter::versDomaine).toList();
    }

    @Override
    public Optional<Quartier> trouverQuartier(Long id) {
        return quartierRepository.findById(id).map(ReferentielRepositoryAdapter::versDomaine);
    }

    @Override
    public boolean quartierExiste(Long id) {
        return quartierRepository.existsById(id);
    }

    private static Quartier versDomaine(QuartierEntity q) {
        return new Quartier(q.getId(), q.getNom(), q.getZone().getId(), q.getLatitude(), q.getLongitude());
    }
}
