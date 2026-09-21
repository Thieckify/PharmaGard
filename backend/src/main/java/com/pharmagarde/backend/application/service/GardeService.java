package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.GardeRequest;
import com.pharmagarde.backend.application.dto.GardeResponse;
import com.pharmagarde.backend.application.dto.PharmacieDeGardeResponse;
import com.pharmagarde.backend.domain.model.Garde;
import com.pharmagarde.backend.infrastructure.persistence.entity.GardeEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import com.pharmagarde.backend.infrastructure.persistence.mapper.GardeMapper;
import com.pharmagarde.backend.infrastructure.persistence.repository.GardeRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.PharmacieRepository;
import com.pharmagarde.backend.infrastructure.security.JwtUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Contient toute la logique métier liée au planning de garde : déclaration,
 * modification, suppression et consultation d'une période (côté pharmacien),
 * et calcul des pharmacies de garde à une date donnée (côté public).
 */
@Service
public class GardeService {

    private final GardeRepository gardeRepository;
    private final PharmacieRepository pharmacieRepository;

    public GardeService(GardeRepository gardeRepository, PharmacieRepository pharmacieRepository) {
        this.gardeRepository = gardeRepository;
        this.pharmacieRepository = pharmacieRepository;
    }

    // Liste les périodes de garde déjà déclarées par la pharmacie du pharmacien connecté.
    public List<GardeResponse> listMesGardes(JwtUserDetails principal) {
        List<PharmacieEntity> pharmacies = pharmacieRepository.findByTenantUserId(principal.userId());
        if (pharmacies.isEmpty()) {
            throw new IllegalArgumentException("Aucune pharmacie associée à ce compte");
        }
        PharmacieEntity pharmacie = pharmacies.get(0);

        return gardeRepository.findByPharmacieId(pharmacie.getId()).stream()
                .map(this::versReponse)
                .toList();
    }

    // Déclare une nouvelle période de garde pour la pharmacie du pharmacien connecté.
    public GardeResponse declarerGarde(GardeRequest request, JwtUserDetails principal) {

        List<PharmacieEntity> pharmacies = pharmacieRepository.findByTenantUserId(principal.userId());
        if (pharmacies.isEmpty()) {
            throw new IllegalArgumentException("Aucune pharmacie associée à ce compte");
        }
        PharmacieEntity pharmacie = pharmacies.get(0);

        verifierPharmacieActive(pharmacie);
        verifierDateDebutPasDansLePasse(request.dateDebut());
        verifierDatesValides(request.dateDebut(), request.dateFin());
        verifierPasDeChevauchement(gardeRepository.findByPharmacieId(pharmacie.getId()),
                request.dateDebut(), request.dateFin());

        GardeEntity entity = new GardeEntity();
        entity.setPharmacie(pharmacie);
        entity.setDateDebut(request.dateDebut());
        entity.setDateFin(request.dateFin());
        entity.setType(request.type());

        return versReponse(gardeRepository.save(entity));
    }

    // Modifie une période de garde existante. Mêmes vérifications que la déclaration,
    // plus une règle spécifique sur la date de début (voir verifierDateDebutModification),
    // et le chevauchement exclut la période qu'on modifie de sa propre comparaison.
    public GardeResponse modifierGarde(Long gardeId, GardeRequest request, JwtUserDetails principal) {

        GardeEntity entity = trouverGardePropre(gardeId, principal);
        PharmacieEntity pharmacie = entity.getPharmacie();

        verifierPharmacieActive(pharmacie);
        verifierDatesValides(request.dateDebut(), request.dateFin());
        verifierDateDebutModification(entity.getDateDebut(), request.dateDebut());

        List<GardeEntity> autresPeriodes = gardeRepository.findByPharmacieId(pharmacie.getId()).stream()
                .filter(g -> !g.getId().equals(gardeId))
                .toList();
        verifierPasDeChevauchement(autresPeriodes, request.dateDebut(), request.dateFin());

        entity.setDateDebut(request.dateDebut());
        entity.setDateFin(request.dateFin());
        entity.setType(request.type());

        return versReponse(gardeRepository.save(entity));
    }

    // Supprime une période de garde existante.
    public void supprimerGarde(Long gardeId, JwtUserDetails principal) {
        GardeEntity entity = trouverGardePropre(gardeId, principal);
        gardeRepository.delete(entity);
    }

    // Calcule les pharmacies de garde à une date donnée. Accessible publiquement (RG-04) :
    // aucune info interne (validation, tenant) n'est renvoyée, et les pharmacies non
    // validées sont exclues même si une période de garde existe sur cette date (RG-05).
    public List<PharmacieDeGardeResponse> calculerPharmaciesDeGarde(LocalDate date) {
        List<GardeEntity> gardesActives = gardeRepository
                .findByDateDebutLessThanEqualAndDateFinGreaterThanEqual(date, date);

        return gardesActives.stream()
                .filter(g -> g.getPharmacie().isActive())
                .map(g -> {
                    PharmacieEntity p = g.getPharmacie();
                    return new PharmacieDeGardeResponse(
                            p.getId(), p.getNom(), p.getAdresse(),
                            p.getQuartier().getNom(), p.getZone().getNom(),
                            p.getLatitude(), p.getLongitude(), p.getTelephone(),
                            p.getHoraireOuverture(), p.getHoraireFermeture(),
                            g.getType(), g.getDateDebut(), g.getDateFin()
                    );
                })
                .toList();
    }

    // Va chercher une garde par id, et vérifie qu'elle appartient bien à la pharmacie
    // du pharmacien connecté (sinon 403) — même principe que le scoping par zone
    // sur PharmacieService.validerPharmacie, mais scopé par tenant ici (RG-01).
    private GardeEntity trouverGardePropre(Long gardeId, JwtUserDetails principal) {
        GardeEntity entity = gardeRepository.findById(gardeId)
                .orElseThrow(() -> new IllegalArgumentException("Garde introuvable : " + gardeId));

        if (!entity.getPharmacie().getTenantUser().getId().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez agir que sur les périodes de garde de votre propre pharmacie");
        }
        return entity;
    }

    private void verifierPharmacieActive(PharmacieEntity pharmacie) {
        if (!pharmacie.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Votre pharmacie doit être validée par un superviseur pour gérer son planning de garde");
        }
    }

    private void verifierDatesValides(LocalDate dateDebut, LocalDate dateFin) {
        if (dateFin.isBefore(dateDebut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être postérieure ou égale à la date de début");
        }
    }

    // Empêche de DÉCLARER une garde qui commence dans le passé — une pharmacie
    // malhonnête ne peut donc jamais fabriquer une "preuve" de garde après coup
    // via une création. Comparaison faite avec l'horloge du SERVEUR, jamais celle
    // du navigateur du pharmacien (qu'il pourrait manipuler).
    private void verifierDateDebutPasDansLePasse(LocalDate dateDebut) {
        if (dateDebut.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de début ne peut pas être dans le passé");
        }
    }

    // Règle affinée pour la MODIFICATION d'une garde existante, pensée pour la
    // transparence (audit) sans bloquer les corrections légitimes :
    // - Garde pas encore commencée : la nouvelle date de début reste libre, tant
    //   qu'elle ne tombe pas elle-même dans le passé.
    // - Garde déjà commencée : la date de début peut seulement être AVANCÉE
    //   (période raccourcie), jamais RECULÉE — sinon on pourrait fabriquer après
    //   coup une garde plus longue que celle réellement déclarée à l'origine.
    //   Dans ce cas précis, seule une suppression + nouvelle déclaration reste possible.
    private void verifierDateDebutModification(LocalDate dateDebutActuelle, LocalDate nouvelleDateDebut) {
        LocalDate aujourdHui = LocalDate.now();
        boolean dejaCommencee = !dateDebutActuelle.isAfter(aujourdHui);

        if (dejaCommencee) {
            if (nouvelleDateDebut.isBefore(dateDebutActuelle)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette garde a déjà commencé : sa date de début peut seulement être avancée, jamais reculée. Supprimez et redéclarez si besoin.");
            }
        } else if (nouvelleDateDebut.isBefore(aujourdHui)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de début ne peut pas être dans le passé");
        }
    }

    // RG-03 : aucune période de la liste fournie ne doit chevaucher [dateDebut, dateFin].
    private void verifierPasDeChevauchement(List<GardeEntity> periodesAComparer, LocalDate dateDebut, LocalDate dateFin) {
        boolean chevauche = periodesAComparer.stream().anyMatch(g ->
                !g.getDateDebut().isAfter(dateFin) && !dateDebut.isAfter(g.getDateFin())
        );
        if (chevauche) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette période chevauche une autre garde déjà déclarée pour votre pharmacie");
        }
    }

    private GardeResponse versReponse(GardeEntity entity) {
        Garde domaine = GardeMapper.toDomain(entity);
        return new GardeResponse(
                domaine.getId(), domaine.getPharmacieId(), entity.getPharmacie().getNom(),
                domaine.getDateDebut(), domaine.getDateFin(), domaine.getType()
        );
    }
}