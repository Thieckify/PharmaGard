package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.PharmacieRequest;
import com.pharmagarde.backend.application.dto.PharmacieResponse;
import com.pharmagarde.backend.application.dto.ValidationRequest;
import com.pharmagarde.backend.domain.enums.Role;
import com.pharmagarde.backend.domain.model.Pharmacie;
import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.QuartierEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.UserEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.ZoneEntity;
import com.pharmagarde.backend.infrastructure.persistence.mapper.PharmacieMapper;
import com.pharmagarde.backend.infrastructure.persistence.repository.GardeRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.PharmacieRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.QuartierRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.UserRepository;
import com.pharmagarde.backend.infrastructure.security.JwtUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contient toute la logique métier liée à la gestion de la fiche pharmacie :
 * création/mise à jour par le pharmacien (upsert), consultation scopée par rôle,
 * validation/refus et suppression par un superviseur.
 */
@Service
public class PharmacieService {

    private static final Logger log = LoggerFactory.getLogger(PharmacieService.class);

    private final PharmacieRepository pharmacieRepository;
    private final QuartierRepository quartierRepository;
    private final UserRepository userRepository;
    private final GardeRepository gardeRepository;

    // Injection par constructeur : Spring fournit automatiquement ces repositories
    // quand il crée le service (pas besoin d'un "new" manuel quelque part)
    public PharmacieService(PharmacieRepository pharmacieRepository,
                             QuartierRepository quartierRepository,
                             UserRepository userRepository,
                             GardeRepository gardeRepository) {
        this.pharmacieRepository = pharmacieRepository;
        this.quartierRepository = quartierRepository;
        this.userRepository = userRepository;
        this.gardeRepository = gardeRepository;
    }

    // Point d'entrée utilisé par le controller : l'identité du pharmacien vient de son JWT.
    public PharmacieResponse upsertPharmacie(PharmacieRequest request, JwtUserDetails principal) {
        return upsertPharmacieDeUtilisateur(request, principal.userId());
    }

    // Crée la fiche pharmacie de l'utilisateur donné si elle n'existe pas, sinon la met à jour.
    // Prend un simple userId (et non le JWT) pour être aussi réutilisable à l'inscription,
    // moment où le JWT n'existe pas encore.
    public PharmacieResponse upsertPharmacieDeUtilisateur(PharmacieRequest request, Long userId) {

        // 1. On va chercher le quartier choisi par le pharmacien.
        // orElseThrow(...) : si aucun quartier ne correspond à cet id, on arrête tout de suite
        // avec une erreur claire, plutôt que de continuer avec une donnée manquante.
        QuartierEntity quartier = quartierRepository.findById(request.quartierId())
                .orElseThrow(() -> new IllegalArgumentException("Quartier introuvable : " + request.quartierId()));

        // 2. La zone se déduit automatiquement du quartier (pas besoin que le pharmacien la précise)
        ZoneEntity zone = quartier.getZone();

        // 3. On vérifie si ce pharmacien a déjà une fiche.
        // findByTenantUserId renvoie une List (potentiellement vide) : on prend le 1er élément s'il existe.
        List<PharmacieEntity> pharmaciesExistantes = pharmacieRepository.findByTenantUserId(userId);

        PharmacieEntity entity;
        if (!pharmaciesExistantes.isEmpty()) {
            entity = pharmaciesExistantes.get(0);

            // Deux changements font tomber la validation, parce qu'ils portent sur ce que le
            // superviseur a contrôlé : la zone (RG-11) et le numéro d'agrément (RG-16).
            boolean changementDeZone = !zone.getId().equals(entity.getZone().getId());

            // RG-16 : un numéro d'agrément différent de l'actuel le remplace, mais la fiche
            // repasse en attente : le superviseur doit revérifier le nouveau numéro. Un numéro
            // vide ou identique (à la casse près) ne change rien.
            String nouveauNumero = request.numeroAgrementOrdre() == null ? "" : request.numeroAgrementOrdre().trim();
            boolean changementDAgrement = !nouveauNumero.isEmpty()
                    && !nouveauNumero.equalsIgnoreCase(entity.getNumeroAgrementOrdre());

            if (changementDeZone || changementDAgrement) {
                entity.setActive(false);
                entity.setValideParUser(null);
                entity.setValideLe(null);
            }

            if (changementDAgrement) {
                // Aucune table d'historique : cette ligne est la seule trace du remplacement
                log.info("Numéro d'agrément modifié : pharmacie id={}, ancien='{}', nouveau='{}' ; fiche remise en attente",
                        entity.getId(), pourLog(entity.getNumeroAgrementOrdre()), pourLog(nouveauNumero));
                entity.setNumeroAgrementOrdre(nouveauNumero);
            }

            entity.setNom(request.nom());
            entity.setAdresse(request.adresse());
            entity.setQuartier(quartier);
            entity.setZone(zone);
            entity.setLatitude(request.latitude());
            entity.setLongitude(request.longitude());
            entity.setTelephone(request.telephone());
            entity.setHoraireOuverture(request.horaireOuverture());
            entity.setHoraireFermeture(request.horaireFermeture());
        } else {
            // Création : le numéro d'agrément est obligatoire (preuve déclarative d'exercice
            // légal). PharmacieRequest ne le valide pas lui-même, puisqu'une mise à jour n'a pas
            // à le renvoyer : c'est donc ici, à la création, qu'on l'exige.
            String numeroAgrement = request.numeroAgrementOrdre();
            if (numeroAgrement == null || numeroAgrement.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le numéro d'agrément est obligatoire à la création de la fiche");
            }

            // Nouvelle fiche, inactive par défaut (en attente de validation)
            UserEntity tenantUser = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));

            entity = new PharmacieEntity();
            entity.setNom(request.nom());
            entity.setAdresse(request.adresse());
            entity.setQuartier(quartier);
            entity.setZone(zone);
            entity.setLatitude(request.latitude());
            entity.setLongitude(request.longitude());
            entity.setTelephone(request.telephone());
            entity.setHoraireOuverture(request.horaireOuverture());
            entity.setHoraireFermeture(request.horaireFermeture());
            entity.setActive(false);
            entity.setTenantUser(tenantUser);
            entity.setNumeroAgrementOrdre(numeroAgrement.trim());
        }

        // 4. Sauvegarde en base (save() fait un INSERT si l'id est vide, un UPDATE sinon)
        PharmacieEntity sauvegardee = pharmacieRepository.save(entity);

        // 5. On transforme la fiche en réponse pour le frontend
        return versReponse(sauvegardee);
    }

    // Liste les pharmacies visibles pour la personne connectée, selon son rôle (RG-01 / RG-02)
    public List<PharmacieResponse> listPharmacies(JwtUserDetails principal) {
        List<PharmacieEntity> entities;

        if ("PHARMACIEN".equals(principal.role())) {
            // RG-01 : le pharmacien ne voit que sa propre pharmacie
            entities = pharmacieRepository.findByTenantUserId(principal.userId());

        } else if ("SUPERVISEUR".equals(principal.role()) && principal.zoneId() != null) {
            // Superviseur de zone : uniquement les pharmacies de sa zone
            entities = pharmacieRepository.findByZoneId(principal.zoneId());

        } else {
            // Superviseur général (zoneId null) : toutes les pharmacies (RG-02)
            entities = pharmacieRepository.findAll();
        }

        return entities.stream().map(this::versReponse).toList();
    }

    // Valide ou refuse une pharmacie (superviseur uniquement), dans les limites de sa zone (RG-02)
    public PharmacieResponse validerPharmacie(Long pharmacieId, ValidationRequest request, JwtUserDetails principal) {

        PharmacieEntity entity = pharmacieRepository.findById(pharmacieId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacie introuvable : " + pharmacieId));

        controlerZoneDuSuperviseur(entity, principal, "valider");

        if (request.valide()) {
            // Validation : on active la fiche et on trace qui a validé, et quand.
            UserEntity superviseur = userRepository.findById(principal.userId())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + principal.userId()));
            entity.setActive(true);
            entity.setValideParUser(superviseur);
            entity.setValideLe(LocalDateTime.now());
        } else {
            // Refus : même reset que RG-11 (changement de zone), on revient à l'état "en attente".
            entity.setActive(false);
            entity.setValideParUser(null);
            entity.setValideLe(null);
        }

        PharmacieEntity sauvegardee = pharmacieRepository.save(entity);
        return versReponse(sauvegardee);
    }

    // RG-15 : un superviseur supprime définitivement une pharmacie (ex. un canular), avec son
    // planning de garde et le compte du pharmacien. Tout se fait dans une seule transaction :
    // si une étape échoue, rien n'est supprimé. Même portée que la validation (RG-02).
    @Transactional
    public void supprimerPharmacie(Long pharmacieId, JwtUserDetails principal) {

        PharmacieEntity pharmacie = pharmacieRepository.findById(pharmacieId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacie introuvable : " + pharmacieId));

        controlerZoneDuSuperviseur(pharmacie, principal, "supprimer");

        UserEntity pharmacien = pharmacie.getTenantUser();

        // Ordre imposé par les clés étrangères : le planning dépend de la fiche, et la fiche
        // dépend du compte. flush() envoie les suppressions à la base avant de toucher au compte.
        gardeRepository.deleteByPharmacieId(pharmacieId);
        pharmacieRepository.delete(pharmacie);
        pharmacieRepository.flush();

        // Le compte n'est supprimé que s'il est bien celui d'un pharmacien : cette route ne doit
        // jamais pouvoir effacer un compte superviseur ou admin, même si les données étaient incohérentes.
        String compte = "conservé (ce n'est pas un compte pharmacien)";
        if (pharmacien != null && pharmacien.getRole() == Role.PHARMACIEN) {
            compte = "supprimé, id=" + pharmacien.getId() + ", email=" + pourLog(pharmacien.getEmail());
            userRepository.delete(pharmacien);
        }

        // Aucune table d'historique : cette ligne est la seule trace de la suppression
        log.warn("Pharmacie supprimée : id={}, nom='{}', agrément='{}' ; compte {} ; par le superviseur id={}",
                pharmacieId, pourLog(pharmacie.getNom()), pourLog(pharmacie.getNumeroAgrementOrdre()),
                compte, principal.userId());
    }

    // RG-02 : un superviseur de zone (zoneId renseigné) ne peut agir que sur les pharmacies de sa
    // propre zone ; le superviseur général (zoneId == null) peut agir sur toutes. 403 explicite :
    // la pharmacie existe bel et bien, l'action est simplement refusée (pas de 404, décision S4).
    private void controlerZoneDuSuperviseur(PharmacieEntity pharmacie, JwtUserDetails principal, String action) {
        boolean estSuperviseurDeZone = principal.zoneId() != null;
        boolean horsDeSaZone = !pharmacie.getZone().getId().equals(principal.zoneId());
        if (estSuperviseurDeZone && horsDeSaZone) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez " + action + " que les pharmacies de votre zone");
        }
    }

    // Nom, email et numéro d'agrément sont saisis par des utilisateurs : on retire les retours à
    // la ligne avant de les écrire dans les logs, sinon quelqu'un pourrait y fabriquer de fausses lignes.
    private static String pourLog(String texte) {
        return texte == null ? "" : texte.replaceAll("[\\r\\n]", "_");
    }

    // Construit la réponse renvoyée au frontend à partir d'une fiche : le domaine fournit les
    // champs simples, et on y ajoute ce qu'il n'a pas (noms de quartier et de zone, infos de
    // validation, numéro d'agrément). Un seul endroit pour les trois routes qui répondent
    // avec une pharmacie (upsert, liste, validation).
    private PharmacieResponse versReponse(PharmacieEntity e) {
        Pharmacie p = PharmacieMapper.toDomain(e);
        Long valideParUserId = e.getValideParUser() != null ? e.getValideParUser().getId() : null;

        return new PharmacieResponse(
                p.getId(), p.getNom(), p.getAdresse(),
                p.getQuartierId(), e.getQuartier().getNom(),
                p.getZoneId(), e.getZone().getNom(),
                p.getLatitude(), p.getLongitude(), p.getTelephone(),
                p.getHoraireOuverture(), p.getHoraireFermeture(),
                p.isActive(), p.getTenantUserId(),
                valideParUserId, e.getValideLe(),
                e.getNumeroAgrementOrdre()
        );
    }
}