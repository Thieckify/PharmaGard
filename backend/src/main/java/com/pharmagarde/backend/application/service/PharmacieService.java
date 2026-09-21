package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.PharmacieRequest;
import com.pharmagarde.backend.application.dto.PharmacieResponse;
import com.pharmagarde.backend.application.dto.ValidationRequest;
import com.pharmagarde.backend.domain.model.Pharmacie;
import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.QuartierEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.UserEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.ZoneEntity;
import com.pharmagarde.backend.infrastructure.persistence.mapper.PharmacieMapper;
import com.pharmagarde.backend.infrastructure.persistence.repository.PharmacieRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.QuartierRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.UserRepository;
import com.pharmagarde.backend.infrastructure.security.JwtUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contient toute la logique métier liée à la gestion de la fiche pharmacie :
 * création/mise à jour par le pharmacien (upsert), consultation scopée par rôle,
 * et validation/refus par un superviseur.
 */
@Service
public class PharmacieService {

    private final PharmacieRepository pharmacieRepository;
    private final QuartierRepository quartierRepository;
    private final UserRepository userRepository;

    // Injection par constructeur : Spring fournit automatiquement ces 3 repositories
    // quand il crée le service (pas besoin d'un "new" manuel quelque part)
    public PharmacieService(PharmacieRepository pharmacieRepository,
                             QuartierRepository quartierRepository,
                             UserRepository userRepository) {
        this.pharmacieRepository = pharmacieRepository;
        this.quartierRepository = quartierRepository;
        this.userRepository = userRepository;
    }

    // Crée la fiche pharmacie du user connecté si elle n'existe pas, sinon la met à jour.
    public PharmacieResponse upsertPharmacie(PharmacieRequest request, JwtUserDetails principal) {

        // 1. On va chercher le quartier choisi par le pharmacien.
        // orElseThrow(...) : si aucun quartier ne correspond à cet id, on arrête tout de suite
        // avec une erreur claire, plutôt que de continuer avec une donnée manquante.
        QuartierEntity quartier = quartierRepository.findById(request.quartierId())
                .orElseThrow(() -> new IllegalArgumentException("Quartier introuvable : " + request.quartierId()));

        // 2. La zone se déduit automatiquement du quartier (pas besoin que le pharmacien la précise)
        ZoneEntity zone = quartier.getZone();

        // 3. On vérifie si ce pharmacien a déjà une fiche.
        // findByTenantUserId renvoie une List (potentiellement vide) : on prend le 1er élément s'il existe.
        List<PharmacieEntity> pharmaciesExistantes = pharmacieRepository.findByTenantUserId(principal.userId());

        PharmacieEntity entity;
                if (!pharmaciesExistantes.isEmpty()) {
            entity = pharmaciesExistantes.get(0);

            // Si la zone change, la validation précédente (donnée par le superviseur de l'ancienne
            // zone) n'a plus autorité sur cette pharmacie : on force une revalidation par le
            // superviseur de la nouvelle zone.
            boolean changementDeZone = !zone.getId().equals(entity.getZone().getId());
            if (changementDeZone) {
                entity.setActive(false);
                entity.setValideParUser(null);
                entity.setValideLe(null);
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
            // Création : nouvelle fiche, inactive par défaut (en attente de validation)
            UserEntity tenantUser = userRepository.findById(principal.userId())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + principal.userId()));

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
            entity.setNumeroAgrementOrdre(request.numeroAgrementOrdre());
        }

        // 4. Sauvegarde en base (save() fait un INSERT si l'id est vide, un UPDATE sinon)
        PharmacieEntity sauvegardee = pharmacieRepository.save(entity);

        // 5. On transforme l'entité JPA en modèle domaine, puis en réponse pour le frontend
        Pharmacie domaine = PharmacieMapper.toDomain(sauvegardee);
        Long valideParUserId = sauvegardee.getValideParUser() != null ? sauvegardee.getValideParUser().getId() : null;
        return versReponse(domaine, quartier, zone, valideParUserId, sauvegardee.getValideLe());
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

        return entities.stream()
                .map(e -> {
                    Long valideParUserId = e.getValideParUser() != null ? e.getValideParUser().getId() : null;
                    return versReponse(PharmacieMapper.toDomain(e), e.getQuartier(), e.getZone(), valideParUserId, e.getValideLe());
                })
                .toList();
    }

    // Valide ou refuse une pharmacie (superviseur uniquement). Un superviseur de zone ne peut
    // agir que sur les pharmacies de sa propre zone (RG-02) ; le superviseur général
    // (zoneId == null) peut agir sur n'importe quelle zone.
    public PharmacieResponse validerPharmacie(Long pharmacieId, ValidationRequest request, JwtUserDetails principal) {

        PharmacieEntity entity = pharmacieRepository.findById(pharmacieId)
                .orElseThrow(() -> new IllegalArgumentException("Pharmacie introuvable : " + pharmacieId));

        // RG-02 appliquée à la validation : un superviseur de zone (zoneId renseigné) est
        // restreint à sa propre zone. On compare la zone de la pharmacie à celle du superviseur.
        boolean estSuperviseurDeZone = principal.zoneId() != null;
        boolean horsDeSaZone = !entity.getZone().getId().equals(principal.zoneId());
        if (estSuperviseurDeZone && horsDeSaZone) {
            // 403 explicite : la pharmacie existe bel et bien, le superviseur n'a simplement
            // pas le droit d'agir dessus (décision prise ensemble juste avant).
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez valider que les pharmacies de votre zone");
        }

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
        Pharmacie domaine = PharmacieMapper.toDomain(sauvegardee);
        Long valideParUserId = sauvegardee.getValideParUser() != null ? sauvegardee.getValideParUser().getId() : null;
        return versReponse(domaine, sauvegardee.getQuartier(), sauvegardee.getZone(), valideParUserId, sauvegardee.getValideLe());
    }

    // Petite méthode utilitaire interne : construit un PharmacieResponse en ajoutant
    // les noms de quartier/zone (que le domaine seul n'a pas, juste des ids), ainsi que
    // les infos de validation (qui a validé, quand).
    private PharmacieResponse versReponse(Pharmacie p, QuartierEntity quartier, ZoneEntity zone,
                                           Long valideParUserId, LocalDateTime valideLe) {
        return new PharmacieResponse(
                p.getId(), p.getNom(), p.getAdresse(),
                p.getQuartierId(), quartier.getNom(),
                p.getZoneId(), zone.getNom(),
                p.getLatitude(), p.getLongitude(), p.getTelephone(),
                p.getHoraireOuverture(), p.getHoraireFermeture(),
                p.isActive(), p.getTenantUserId(),
                valideParUserId, valideLe
        );
    }
}