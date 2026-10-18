package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.GardeRequest;
import com.pharmagarde.backend.application.dto.GardeResponse;
import com.pharmagarde.backend.application.dto.PharmacieDeGardeResponse;
import com.pharmagarde.backend.application.support.LibellesReferentiel;
import com.pharmagarde.backend.domain.exception.AccesRefuseException;
import com.pharmagarde.backend.domain.exception.ConflitMetierException;
import com.pharmagarde.backend.domain.exception.DonneeInvalideException;
import com.pharmagarde.backend.domain.exception.RessourceIntrouvableException;
import com.pharmagarde.backend.domain.model.Garde;
import com.pharmagarde.backend.domain.model.Pharmacie;
import com.pharmagarde.backend.domain.model.UtilisateurConnecte;
import com.pharmagarde.backend.domain.port.out.GardeRepositoryPort;
import com.pharmagarde.backend.domain.port.out.PharmacieRepositoryPort;
import com.pharmagarde.backend.domain.port.out.ReferentielRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contient toute la logique métier liée au planning de garde : déclaration,
 * modification, suppression et consultation d'une période (côté pharmacien),
 * et calcul des pharmacies de garde à une date donnée (côté public).
 */
@Service
public class GardeService {

    private final GardeRepositoryPort gardes;
    private final PharmacieRepositoryPort pharmacies;
    private final ReferentielRepositoryPort referentiel;

    public GardeService(GardeRepositoryPort gardes, PharmacieRepositoryPort pharmacies,
                        ReferentielRepositoryPort referentiel) {
        this.gardes = gardes;
        this.pharmacies = pharmacies;
        this.referentiel = referentiel;
    }

    // Liste les périodes de garde déjà déclarées par la pharmacie du pharmacien connecté.
    public List<GardeResponse> listMesGardes(UtilisateurConnecte utilisateur) {
        Pharmacie pharmacie = pharmacieDe(utilisateur);
        return gardes.listerParPharmacie(pharmacie.getId()).stream()
                .map(g -> versReponse(g, pharmacie))
                .toList();
    }

    // Déclare une nouvelle période de garde pour la pharmacie du pharmacien connecté.
    public GardeResponse declarerGarde(GardeRequest request, UtilisateurConnecte utilisateur) {
        Pharmacie pharmacie = pharmacieDe(utilisateur);

        verifierPharmacieActive(pharmacie);
        verifierDateDebutPasDansLePasse(request.dateDebut());
        verifierDatesValides(request.dateDebut(), request.dateFin());
        verifierPasDeChevauchement(gardes.listerParPharmacie(pharmacie.getId()),
                request.dateDebut(), request.dateFin());

        Garde nouvelle = new Garde(null, pharmacie.getId(), request.dateDebut(), request.dateFin(), request.type());
        return versReponse(gardes.enregistrer(nouvelle), pharmacie);
    }

    // Modifie une période de garde qui n'a pas encore commencé (RG-06). Mêmes vérifications que
    // la déclaration ; le chevauchement exclut la période modifiée de sa propre comparaison.
    public GardeResponse modifierGarde(Long gardeId, GardeRequest request, UtilisateurConnecte utilisateur) {
        Garde garde = trouverGardePropre(gardeId, utilisateur);
        Pharmacie pharmacie = pharmacies.trouverParId(garde.getPharmacieId())
                .orElseThrow(() -> new RessourceIntrouvableException("Pharmacie introuvable : " + garde.getPharmacieId()));

        verifierPasEncoreCommencee(garde);
        verifierPharmacieActive(pharmacie);
        verifierDateDebutPasDansLePasse(request.dateDebut());
        verifierDatesValides(request.dateDebut(), request.dateFin());

        List<Garde> autresPeriodes = gardes.listerParPharmacie(pharmacie.getId()).stream()
                .filter(g -> !g.getId().equals(gardeId))
                .toList();
        verifierPasDeChevauchement(autresPeriodes, request.dateDebut(), request.dateFin());

        garde.setDateDebut(request.dateDebut());
        garde.setDateFin(request.dateFin());
        garde.setType(request.type());
        return versReponse(gardes.enregistrer(garde), pharmacie);
    }

    // Supprime une période de garde qui n'a pas encore commencé (RG-06).
    public void supprimerGarde(Long gardeId, UtilisateurConnecte utilisateur) {
        Garde garde = trouverGardePropre(gardeId, utilisateur);
        verifierPasEncoreCommencee(garde);
        gardes.supprimer(garde.getId());
    }

    // Calcule les pharmacies de garde à une date donnée. Accessible publiquement (RG-04) :
    // aucune info interne (validation, tenant) n'est renvoyée, et les pharmacies non
    // validées sont exclues même si une période de garde existe sur cette date (RG-05).
    public List<PharmacieDeGardeResponse> calculerPharmaciesDeGarde(LocalDate date) {
        List<Garde> gardesDuJour = gardes.listerEnCoursLe(date);

        Map<Long, Pharmacie> parId = pharmacies.trouverParIds(
                        gardesDuJour.stream().map(Garde::getPharmacieId).distinct().toList()).stream()
                .collect(Collectors.toMap(Pharmacie::getId, Function.identity()));
        LibellesReferentiel libelles = LibellesReferentiel.charger(referentiel);

        return gardesDuJour.stream()
                .filter(g -> parId.containsKey(g.getPharmacieId()) && parId.get(g.getPharmacieId()).isActive())
                .map(g -> {
                    Pharmacie p = parId.get(g.getPharmacieId());
                    return new PharmacieDeGardeResponse(
                            p.getId(), p.getNom(), p.getAdresse(),
                            libelles.nomQuartier(p.getQuartierId()), libelles.nomZone(p.getZoneId()),
                            p.getLatitude(), p.getLongitude(), p.getTelephone(),
                            p.getHoraireOuverture(), p.getHoraireFermeture(),
                            g.getType(), g.getDateDebut(), g.getDateFin());
                })
                .toList();
    }

    // La pharmacie dont le pharmacien connecté est le tenant (RG-01)
    private Pharmacie pharmacieDe(UtilisateurConnecte utilisateur) {
        return pharmacies.trouverParTenant(utilisateur.userId())
                .orElseThrow(() -> new RessourceIntrouvableException("Aucune pharmacie associée à ce compte"));
    }

    // Va chercher une garde par id, et vérifie qu'elle appartient bien à la pharmacie
    // du pharmacien connecté (sinon 403) — scoping par tenant (RG-01).
    private Garde trouverGardePropre(Long gardeId, UtilisateurConnecte utilisateur) {
        Garde garde = gardes.trouverParId(gardeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Garde introuvable : " + gardeId));

        boolean estProprietaire = pharmacies.trouverParId(garde.getPharmacieId())
                .map(p -> utilisateur.userId().equals(p.getTenantUserId()))
                .orElse(false);
        if (!estProprietaire) {
            throw new AccesRefuseException("Vous ne pouvez agir que sur les périodes de garde de votre propre pharmacie");
        }
        return garde;
    }

    // RG-06 : une garde ne peut être modifiée ou supprimée que si elle n'a pas commencé. Une fois
    // commencée, elle fait foi auprès du public et du superviseur : elle ne bouge plus.
    // Comparaison faite avec l'horloge du SERVEUR, jamais celle du navigateur.
    private void verifierPasEncoreCommencee(Garde garde) {
        if (!garde.getDateDebut().isAfter(LocalDate.now())) {
            throw new ConflitMetierException(
                    "Cette garde a déjà commencé : elle ne peut plus être modifiée ni supprimée");
        }
    }

    private void verifierPharmacieActive(Pharmacie pharmacie) {
        if (!pharmacie.isActive()) {
            throw new ConflitMetierException(
                    "Votre pharmacie doit être validée par un superviseur pour gérer son planning de garde");
        }
    }

    private void verifierDatesValides(LocalDate dateDebut, LocalDate dateFin) {
        if (dateFin.isBefore(dateDebut)) {
            throw new DonneeInvalideException("La date de fin doit être postérieure ou égale à la date de début");
        }
    }

    // RG-13 : une garde ne peut jamais commencer dans le passé — une pharmacie malhonnête ne
    // peut donc pas fabriquer une "preuve" de garde après coup.
    private void verifierDateDebutPasDansLePasse(LocalDate dateDebut) {
        if (dateDebut.isBefore(LocalDate.now())) {
            throw new DonneeInvalideException("La date de début ne peut pas être dans le passé");
        }
    }

    // RG-03 : aucune période de la liste fournie ne doit chevaucher [dateDebut, dateFin].
    private void verifierPasDeChevauchement(List<Garde> periodesAComparer, LocalDate dateDebut, LocalDate dateFin) {
        boolean chevauche = periodesAComparer.stream().anyMatch(g ->
                !g.getDateDebut().isAfter(dateFin) && !dateDebut.isAfter(g.getDateFin()));
        if (chevauche) {
            throw new ConflitMetierException("Cette période chevauche une autre garde déjà déclarée pour votre pharmacie");
        }
    }

    private GardeResponse versReponse(Garde garde, Pharmacie pharmacie) {
        return new GardeResponse(garde.getId(), garde.getPharmacieId(), pharmacie.getNom(),
                garde.getDateDebut(), garde.getDateFin(), garde.getType());
    }
}
