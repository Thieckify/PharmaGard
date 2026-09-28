package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.PharmaciePubliqueResponse;
import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import com.pharmagarde.backend.infrastructure.persistence.repository.PharmacieRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.QuartierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logique de la recherche publique de pharmacies (sans authentification, RG-04).
 * Séparé de PharmacieService, qui gère les fiches des pharmaciens connectés.
 * Seules les pharmacies actives sont jamais renvoyées (RG-05).
 */
@Service
public class PharmacieRechercheService {

    private final PharmacieRepository pharmacieRepository;
    private final QuartierRepository quartierRepository;

    public PharmacieRechercheService(PharmacieRepository pharmacieRepository,
                                      QuartierRepository quartierRepository) {
        this.pharmacieRepository = pharmacieRepository;
        this.quartierRepository = quartierRepository;
    }

    // Sans quartier : toutes les pharmacies actives. Avec quartier : uniquement celles de ce quartier.
    // readOnly = true : la base est ouverte en lecture seule le temps de la méthode, ce qui permet
    // aussi de lire le nom du quartier et de la zone (chargés à la demande) sans erreur.
    @Transactional(readOnly = true)
    public List<PharmaciePubliqueResponse> rechercher(Long quartierId) {
        List<PharmacieEntity> pharmacies;

        if (quartierId == null) {
            pharmacies = pharmacieRepository.findByActiveTrueOrderByNomAsc();
        } else {
            // Un quartier qui n'existe pas est une erreur (404) ; un quartier existant
            // sans aucune pharmacie active renvoie simplement une liste vide.
            if (!quartierRepository.existsById(quartierId)) {
                throw new IllegalArgumentException("Quartier introuvable : " + quartierId);
            }
            pharmacies = pharmacieRepository.findByQuartierIdAndActiveTrueOrderByNomAsc(quartierId);
        }

        return pharmacies.stream().map(this::versReponse).toList();
    }

    // Transforme une entité en réponse publique (sans aucun champ interne)
    private PharmaciePubliqueResponse versReponse(PharmacieEntity p) {
        return new PharmaciePubliqueResponse(
                p.getId(),
                p.getNom(),
                p.getAdresse(),
                p.getQuartier().getNom(),
                p.getZone().getNom(),
                p.getLatitude(),
                p.getLongitude(),
                p.getTelephone(),
                p.getHoraireOuverture(),
                p.getHoraireFermeture()
        );
    }
}