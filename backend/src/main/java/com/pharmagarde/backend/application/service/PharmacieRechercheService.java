package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.PharmaciePubliqueResponse;
import com.pharmagarde.backend.application.support.LibellesReferentiel;
import com.pharmagarde.backend.domain.exception.RessourceIntrouvableException;
import com.pharmagarde.backend.domain.model.Pharmacie;
import com.pharmagarde.backend.domain.port.out.PharmacieRepositoryPort;
import com.pharmagarde.backend.domain.port.out.ReferentielRepositoryPort;
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

    private final PharmacieRepositoryPort pharmacies;
    private final ReferentielRepositoryPort referentiel;

    public PharmacieRechercheService(PharmacieRepositoryPort pharmacies, ReferentielRepositoryPort referentiel) {
        this.pharmacies = pharmacies;
        this.referentiel = referentiel;
    }

    // Sans quartier : toutes les pharmacies actives. Avec quartier : uniquement celles de ce quartier.
    @Transactional(readOnly = true)
    public List<PharmaciePubliqueResponse> rechercher(Long quartierId) {
        List<Pharmacie> resultats;

        if (quartierId == null) {
            resultats = pharmacies.listerActives();
        } else {
            // Un quartier qui n'existe pas est une erreur (404) ; un quartier existant
            // sans aucune pharmacie active renvoie simplement une liste vide.
            if (!referentiel.quartierExiste(quartierId)) {
                throw new RessourceIntrouvableException("Quartier introuvable : " + quartierId);
            }
            resultats = pharmacies.listerActivesParQuartier(quartierId);
        }

        LibellesReferentiel libelles = LibellesReferentiel.charger(referentiel);
        return resultats.stream().map(p -> versReponse(p, libelles)).toList();
    }

    // Transforme une pharmacie en réponse publique (sans aucun champ interne)
    private PharmaciePubliqueResponse versReponse(Pharmacie p, LibellesReferentiel libelles) {
        return new PharmaciePubliqueResponse(
                p.getId(), p.getNom(), p.getAdresse(),
                libelles.nomQuartier(p.getQuartierId()), libelles.nomZone(p.getZoneId()),
                p.getLatitude(), p.getLongitude(), p.getTelephone(),
                p.getHoraireOuverture(), p.getHoraireFermeture());
    }
}
