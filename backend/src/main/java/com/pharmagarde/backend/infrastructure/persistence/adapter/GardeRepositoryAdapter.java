package com.pharmagarde.backend.infrastructure.persistence.adapter;

import com.pharmagarde.backend.domain.exception.RessourceIntrouvableException;
import com.pharmagarde.backend.domain.model.Garde;
import com.pharmagarde.backend.domain.port.out.GardeRepositoryPort;
import com.pharmagarde.backend.infrastructure.persistence.entity.GardeEntity;
import com.pharmagarde.backend.infrastructure.persistence.entity.PharmacieEntity;
import com.pharmagarde.backend.infrastructure.persistence.mapper.GardeMapper;
import com.pharmagarde.backend.infrastructure.persistence.repository.GardeRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.PharmacieRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adaptateur JPA du port GardeRepositoryPort.
 */
@Component
public class GardeRepositoryAdapter implements GardeRepositoryPort {

    private final GardeRepository gardeRepository;
    private final PharmacieRepository pharmacieRepository;

    public GardeRepositoryAdapter(GardeRepository gardeRepository, PharmacieRepository pharmacieRepository) {
        this.gardeRepository = gardeRepository;
        this.pharmacieRepository = pharmacieRepository;
    }

    @Override
    public Optional<Garde> trouverParId(Long id) {
        return gardeRepository.findById(id).map(GardeMapper::toDomain);
    }

    @Override
    public List<Garde> listerParPharmacie(Long pharmacieId) {
        return gardeRepository.findByPharmacieId(pharmacieId).stream().map(GardeMapper::toDomain).toList();
    }

    @Override
    public List<Garde> listerEnCoursLe(LocalDate date) {
        return gardeRepository.findByDateDebutLessThanEqualAndDateFinGreaterThanEqual(date, date).stream()
                .map(GardeMapper::toDomain)
                .toList();
    }

    @Override
    public Garde enregistrer(Garde garde) {
        PharmacieEntity pharmacie = pharmacieRepository.findById(garde.getPharmacieId())
                .orElseThrow(() -> new RessourceIntrouvableException("Pharmacie introuvable : " + garde.getPharmacieId()));

        GardeEntity entity = new GardeEntity();
        entity.setId(garde.getId());
        entity.setPharmacie(pharmacie);
        entity.setDateDebut(garde.getDateDebut());
        entity.setDateFin(garde.getDateFin());
        entity.setType(garde.getType());
        return GardeMapper.toDomain(gardeRepository.save(entity));
    }

    @Override
    public void supprimer(Long id) {
        gardeRepository.deleteById(id);
    }

    @Override
    public void supprimerParPharmacie(Long pharmacieId) {
        gardeRepository.deleteByPharmacieId(pharmacieId);
    }
}
