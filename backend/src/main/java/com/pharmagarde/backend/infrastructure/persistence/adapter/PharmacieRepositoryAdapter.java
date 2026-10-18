package com.pharmagarde.backend.infrastructure.persistence.adapter;

import com.pharmagarde.backend.domain.exception.RessourceIntrouvableException;
import com.pharmagarde.backend.domain.model.Pharmacie;
import com.pharmagarde.backend.domain.port.out.PharmacieRepositoryPort;
import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.QuartierEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.UserEntity;
import com.pharmagarde.backend.infrastructure.persistence.mapper.PharmacieMapper;
import com.pharmagarde.backend.infrastructure.persistence.repository.PharmacieRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.QuartierRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port PharmacieRepositoryPort : traduit les demandes de la couche application
 * en requêtes Spring Data, et les entités en objets du domaine (et inversement) via PharmacieMapper.
 */
@Component
public class PharmacieRepositoryAdapter implements PharmacieRepositoryPort {

    private final PharmacieRepository pharmacieRepository;
    private final QuartierRepository quartierRepository;
    private final UserRepository userRepository;

    public PharmacieRepositoryAdapter(PharmacieRepository pharmacieRepository,
                                      QuartierRepository quartierRepository,
                                      UserRepository userRepository) {
        this.pharmacieRepository = pharmacieRepository;
        this.quartierRepository = quartierRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<Pharmacie> trouverParId(Long id) {
        return pharmacieRepository.findById(id).map(PharmacieMapper::toDomain);
    }

    @Override
    public List<Pharmacie> trouverParIds(Collection<Long> ids) {
        return versDomaine(pharmacieRepository.findAllById(ids));
    }

    @Override
    public Optional<Pharmacie> trouverParTenant(Long tenantUserId) {
        return pharmacieRepository.findByTenantUserId(tenantUserId).stream().findFirst().map(PharmacieMapper::toDomain);
    }

    @Override
    public List<Pharmacie> listerToutes() {
        return versDomaine(pharmacieRepository.findAll());
    }

    @Override
    public List<Pharmacie> listerParZone(Long zoneId) {
        return versDomaine(pharmacieRepository.findByZoneId(zoneId));
    }

    @Override
    public List<Pharmacie> listerActives() {
        return versDomaine(pharmacieRepository.findByActiveTrueOrderByNomAsc());
    }

    @Override
    public List<Pharmacie> listerActivesParQuartier(Long quartierId) {
        return versDomaine(pharmacieRepository.findByQuartierIdAndActiveTrueOrderByNomAsc(quartierId));
    }

    // Les relations (quartier, zone, propriétaire, validateur) sont rechargées à partir de leurs
    // identifiants : le domaine ne manipule que des identifiants, l'entité JPA des relations.
    @Override
    public Pharmacie enregistrer(Pharmacie pharmacie) {
        QuartierEntity quartier = quartierRepository.findById(pharmacie.getQuartierId())
                .orElseThrow(() -> new RessourceIntrouvableException("Quartier introuvable : " + pharmacie.getQuartierId()));
        UserEntity proprietaire = trouverUtilisateur(pharmacie.getTenantUserId());
        UserEntity validateur = pharmacie.getValideParUserId() != null ? trouverUtilisateur(pharmacie.getValideParUserId()) : null;

        PharmacieEntity entity = PharmacieMapper.toEntity(pharmacie, quartier, quartier.getZone(), proprietaire, validateur);
        return PharmacieMapper.toDomain(pharmacieRepository.save(entity));
    }

    // flush() envoie la suppression à la base tout de suite : un appelant qui supprime ensuite le
    // compte du pharmacien (RG-15) ne doit pas se heurter à la clé étrangère de la fiche.
    @Override
    public void supprimer(Long id) {
        pharmacieRepository.deleteById(id);
        pharmacieRepository.flush();
    }

    private UserEntity trouverUtilisateur(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + id));
    }

    private static List<Pharmacie> versDomaine(List<PharmacieEntity> entities) {
        return entities.stream().map(PharmacieMapper::toDomain).toList();
    }
}
