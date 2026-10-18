package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.PharmacieRequest;
import com.pharmagarde.backend.application.dto.PharmacieResponse;
import com.pharmagarde.backend.application.dto.ValidationRequest;
import com.pharmagarde.backend.application.support.LibellesReferentiel;
import com.pharmagarde.backend.domain.enums.Role;
import com.pharmagarde.backend.domain.exception.AccesRefuseException;
import com.pharmagarde.backend.domain.exception.DonneeInvalideException;
import com.pharmagarde.backend.domain.exception.RessourceIntrouvableException;
import com.pharmagarde.backend.domain.model.Pharmacie;
import com.pharmagarde.backend.domain.model.Quartier;
import com.pharmagarde.backend.domain.model.User;
import com.pharmagarde.backend.domain.model.UtilisateurConnecte;
import com.pharmagarde.backend.domain.port.out.GardeRepositoryPort;
import com.pharmagarde.backend.domain.port.out.PharmacieRepositoryPort;
import com.pharmagarde.backend.domain.port.out.ReferentielRepositoryPort;
import com.pharmagarde.backend.domain.port.out.UtilisateurRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contient toute la logique métier liée à la gestion de la fiche pharmacie :
 * création/mise à jour par le pharmacien (upsert), consultation scopée par rôle,
 * validation/refus et suppression par un superviseur.
 */
@Service
public class PharmacieService {

    private static final Logger log = LoggerFactory.getLogger(PharmacieService.class);

    private final PharmacieRepositoryPort pharmacies;
    private final ReferentielRepositoryPort referentiel;
    private final UtilisateurRepositoryPort utilisateurs;
    private final GardeRepositoryPort gardes;

    // Injection par constructeur : Spring fournit l'implémentation de chaque port
    // (les adaptateurs JPA de la couche infrastructure), le service n'en connaît que l'interface.
    public PharmacieService(PharmacieRepositoryPort pharmacies,
                            ReferentielRepositoryPort referentiel,
                            UtilisateurRepositoryPort utilisateurs,
                            GardeRepositoryPort gardes) {
        this.pharmacies = pharmacies;
        this.referentiel = referentiel;
        this.utilisateurs = utilisateurs;
        this.gardes = gardes;
    }

    // Point d'entrée utilisé par le controller : l'identité du pharmacien vient de son jeton.
    public PharmacieResponse upsertPharmacie(PharmacieRequest request, UtilisateurConnecte utilisateur) {
        return upsertPharmacieDeUtilisateur(request, utilisateur.userId());
    }

    // Crée la fiche pharmacie de l'utilisateur donné si elle n'existe pas, sinon la met à jour.
    // Prend un simple userId pour être aussi réutilisable à l'inscription, moment où le jeton
    // n'existe pas encore.
    public PharmacieResponse upsertPharmacieDeUtilisateur(PharmacieRequest request, Long userId) {

        // 1. Le quartier choisi doit exister ; la zone s'en déduit (jamais saisie par le client)
        Quartier quartier = referentiel.trouverQuartier(request.quartierId())
                .orElseThrow(() -> new RessourceIntrouvableException("Quartier introuvable : " + request.quartierId()));
        Long zoneId = quartier.getZoneId();

        // 2. Ce pharmacien a-t-il déjà une fiche ?
        Optional<Pharmacie> existante = pharmacies.trouverParTenant(userId);

        Pharmacie pharmacie;
        if (existante.isPresent()) {
            pharmacie = existante.get();

            // Deux changements font tomber la validation, parce qu'ils portent sur ce que le
            // superviseur a contrôlé : la zone (RG-11) et le numéro d'agrément (RG-16).
            boolean changementDeZone = !zoneId.equals(pharmacie.getZoneId());

            // RG-16 : un numéro d'agrément différent de l'actuel le remplace, mais la fiche
            // repasse en attente. Un numéro vide ou identique (à la casse près) ne change rien.
            String nouveauNumero = request.numeroAgrementOrdre() == null ? "" : request.numeroAgrementOrdre().trim();
            boolean changementDAgrement = !nouveauNumero.isEmpty()
                    && !nouveauNumero.equalsIgnoreCase(pharmacie.getNumeroAgrementOrdre());

            if (changementDeZone || changementDAgrement) {
                remettreEnAttente(pharmacie);
            }

            if (changementDAgrement) {
                // Aucune table d'historique : cette ligne est la seule trace du remplacement
                log.info("Numéro d'agrément modifié : pharmacie id={}, ancien='{}', nouveau='{}' ; fiche remise en attente",
                        pharmacie.getId(), pourLog(pharmacie.getNumeroAgrementOrdre()), pourLog(nouveauNumero));
                pharmacie.setNumeroAgrementOrdre(nouveauNumero);
            }
        } else {
            // Création : le numéro d'agrément est obligatoire (preuve déclarative d'exercice
            // légal). PharmacieRequest ne l'exige pas, puisqu'une mise à jour n'a pas à le renvoyer.
            String numeroAgrement = request.numeroAgrementOrdre();
            if (numeroAgrement == null || numeroAgrement.isBlank()) {
                throw new DonneeInvalideException("Le numéro d'agrément est obligatoire à la création de la fiche");
            }

            // Nouvelle fiche, inactive par défaut (en attente de validation)
            pharmacie = new Pharmacie();
            pharmacie.setActive(false);
            pharmacie.setTenantUserId(userId);
            pharmacie.setNumeroAgrementOrdre(numeroAgrement.trim());
        }

        pharmacie.setNom(request.nom());
        pharmacie.setAdresse(request.adresse());
        pharmacie.setQuartierId(quartier.getId());
        pharmacie.setZoneId(zoneId);
        pharmacie.setLatitude(request.latitude());
        pharmacie.setLongitude(request.longitude());
        pharmacie.setTelephone(request.telephone());
        pharmacie.setHoraireOuverture(request.horaireOuverture());
        pharmacie.setHoraireFermeture(request.horaireFermeture());

        return versReponse(pharmacies.enregistrer(pharmacie), LibellesReferentiel.charger(referentiel));
    }

    // Liste les pharmacies visibles pour la personne connectée, selon son rôle (RG-01 / RG-02)
    public List<PharmacieResponse> listPharmacies(UtilisateurConnecte utilisateur) {
        List<Pharmacie> visibles;

        if (utilisateur.estPharmacien()) {
            // RG-01 : le pharmacien ne voit que sa propre pharmacie
            visibles = pharmacies.trouverParTenant(utilisateur.userId()).stream().toList();
        } else if (utilisateur.estSuperviseurDeZone()) {
            // Superviseur de zone : uniquement les pharmacies de sa zone
            visibles = pharmacies.listerParZone(utilisateur.zoneId());
        } else {
            // Superviseur général (zone vide) : toutes les pharmacies (RG-02)
            visibles = pharmacies.listerToutes();
        }

        LibellesReferentiel libelles = LibellesReferentiel.charger(referentiel);
        return visibles.stream().map(p -> versReponse(p, libelles)).toList();
    }

    // Valide ou refuse une pharmacie (superviseur uniquement), dans les limites de sa zone (RG-02)
    public PharmacieResponse validerPharmacie(Long pharmacieId, ValidationRequest request, UtilisateurConnecte utilisateur) {
        Pharmacie pharmacie = trouverPharmacie(pharmacieId);
        controlerZoneDuSuperviseur(pharmacie, utilisateur, "valider");

        if (request.valide()) {
            // Validation : on active la fiche et on trace qui a validé, et quand.
            pharmacie.setActive(true);
            pharmacie.setValideParUserId(utilisateur.userId());
            pharmacie.setValideLe(LocalDateTime.now());
        } else {
            // Refus : même retour à l'état "en attente" que RG-11.
            remettreEnAttente(pharmacie);
        }

        return versReponse(pharmacies.enregistrer(pharmacie), LibellesReferentiel.charger(referentiel));
    }

    // RG-15 : un superviseur supprime définitivement une pharmacie (ex. un canular), avec son
    // planning de garde et le compte du pharmacien. Tout se fait dans une seule transaction :
    // si une étape échoue, rien n'est supprimé. Même portée que la validation (RG-02).
    @Transactional
    public void supprimerPharmacie(Long pharmacieId, UtilisateurConnecte utilisateur) {
        Pharmacie pharmacie = trouverPharmacie(pharmacieId);
        controlerZoneDuSuperviseur(pharmacie, utilisateur, "supprimer");

        Optional<User> pharmacien = utilisateurs.trouverParId(pharmacie.getTenantUserId());

        // Ordre imposé par les clés étrangères : le planning dépend de la fiche, la fiche du compte.
        gardes.supprimerParPharmacie(pharmacieId);
        pharmacies.supprimer(pharmacieId);

        // Le compte n'est supprimé que s'il est bien celui d'un pharmacien : cette route ne doit
        // jamais pouvoir effacer un compte superviseur ou admin, même si les données étaient incohérentes.
        String compte = "conservé (ce n'est pas un compte pharmacien)";
        if (pharmacien.isPresent() && pharmacien.get().getRole() == Role.PHARMACIEN) {
            compte = "supprimé, id=" + pharmacien.get().getId() + ", email=" + pourLog(pharmacien.get().getEmail());
            utilisateurs.supprimer(pharmacien.get().getId());
        }

        // Aucune table d'historique : cette ligne est la seule trace de la suppression
        log.warn("Pharmacie supprimée : id={}, nom='{}', agrément='{}' ; compte {} ; par le superviseur id={}",
                pharmacieId, pourLog(pharmacie.getNom()), pourLog(pharmacie.getNumeroAgrementOrdre()),
                compte, utilisateur.userId());
    }

    private Pharmacie trouverPharmacie(Long pharmacieId) {
        return pharmacies.trouverParId(pharmacieId)
                .orElseThrow(() -> new RessourceIntrouvableException("Pharmacie introuvable : " + pharmacieId));
    }

    // État "en attente" : fiche invisible du public, sans trace de validation (RG-11, RG-16, refus)
    private void remettreEnAttente(Pharmacie pharmacie) {
        pharmacie.setActive(false);
        pharmacie.setValideParUserId(null);
        pharmacie.setValideLe(null);
    }

    // RG-02 : un superviseur de zone ne peut agir que sur les pharmacies de sa propre zone ; le
    // superviseur général peut agir sur toutes. 403 explicite : la pharmacie existe bel et bien,
    // l'action est simplement refusée (pas de 404, décision S4).
    private void controlerZoneDuSuperviseur(Pharmacie pharmacie, UtilisateurConnecte utilisateur, String action) {
        if (utilisateur.estSuperviseurDeZone() && !pharmacie.getZoneId().equals(utilisateur.zoneId())) {
            throw new AccesRefuseException("Vous ne pouvez " + action + " que les pharmacies de votre zone");
        }
    }

    // Nom, email et numéro d'agrément sont saisis par des utilisateurs : on retire les retours à
    // la ligne avant de les écrire dans les logs, sinon quelqu'un pourrait y fabriquer de fausses lignes.
    private static String pourLog(String texte) {
        return texte == null ? "" : texte.replaceAll("[\\r\\n]", "_");
    }

    // Construit la réponse renvoyée au frontend : le domaine fournit les champs, l'index du
    // référentiel les noms de quartier et de zone. Un seul endroit pour toutes les routes.
    private PharmacieResponse versReponse(Pharmacie p, LibellesReferentiel libelles) {
        return new PharmacieResponse(
                p.getId(), p.getNom(), p.getAdresse(),
                p.getQuartierId(), libelles.nomQuartier(p.getQuartierId()),
                p.getZoneId(), libelles.nomZone(p.getZoneId()),
                p.getLatitude(), p.getLongitude(), p.getTelephone(),
                p.getHoraireOuverture(), p.getHoraireFermeture(),
                p.isActive(), p.getTenantUserId(),
                p.getValideParUserId(), p.getValideLe(),
                p.getNumeroAgrementOrdre());
    }
}
