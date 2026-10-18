package com.pharmagarde.backend.domain.port.out;

import com.pharmagarde.backend.domain.model.Garde;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port de sortie pour les périodes de garde.
 */
public interface GardeRepositoryPort {

    Optional<Garde> trouverParId(Long id);

    List<Garde> listerParPharmacie(Long pharmacieId);

    // Gardes dont la période contient la date donnée
    List<Garde> listerEnCoursLe(LocalDate date);

    Garde enregistrer(Garde garde);

    void supprimer(Long id);

    void supprimerParPharmacie(Long pharmacieId);
}
