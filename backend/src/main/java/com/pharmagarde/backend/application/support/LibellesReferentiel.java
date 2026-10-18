package com.pharmagarde.backend.application.support;

import com.pharmagarde.backend.domain.model.Quartier;
import com.pharmagarde.backend.domain.model.Zone;
import com.pharmagarde.backend.domain.port.out.ReferentielRepositoryPort;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Noms des quartiers et des zones, indexés par identifiant. Le domaine ne manipule que des
 * identifiants ; les réponses envoyées au frontend affichent des noms. Ce petit index fait le
 * lien, chargé une fois par requête (11 zones et 39 quartiers : coût négligeable).
 */
public final class LibellesReferentiel {

    private final Map<Long, Quartier> quartiers;
    private final Map<Long, String> zones;

    private LibellesReferentiel(Map<Long, Quartier> quartiers, Map<Long, String> zones) {
        this.quartiers = quartiers;
        this.zones = zones;
    }

    public static LibellesReferentiel charger(ReferentielRepositoryPort referentiel) {
        return new LibellesReferentiel(
                referentiel.listerQuartiers().stream().collect(Collectors.toMap(Quartier::getId, Function.identity())),
                referentiel.listerZones().stream().collect(Collectors.toMap(Zone::getId, Zone::getNom)));
    }

    public String nomQuartier(Long quartierId) {
        Quartier quartier = quartiers.get(quartierId);
        return quartier != null ? quartier.getNom() : null;
    }

    public String nomZone(Long zoneId) {
        return zones.get(zoneId);
    }
}
