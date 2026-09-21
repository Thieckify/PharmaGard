package com.pharmagarde.backend.application.dto;

import com.pharmagarde.backend.domain.enums.TypeGarde;

import java.time.LocalDate;

/**
 * Représentation d'une période de garde renvoyée au frontend, avec le nom de la
 * pharmacie déjà résolu (même logique que PharmacieResponse pour quartier/zone).
 */
public record GardeResponse(
        Long id,
        Long pharmacieId,
        String nomPharmacie,
        LocalDate dateDebut,
        LocalDate dateFin,
        TypeGarde type
) {
}