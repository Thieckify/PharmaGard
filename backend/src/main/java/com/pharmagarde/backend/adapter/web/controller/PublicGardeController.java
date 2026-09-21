package com.pharmagarde.backend.adapter.web.controller;

import com.pharmagarde.backend.application.dto.PharmacieDeGardeResponse;
import com.pharmagarde.backend.application.service.GardeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint public (sans authentification, RG-04) pour consulter les pharmacies
 * de garde à une date donnée. Toute la logique (RG-05 inclus) vit dans GardeService.
 */
@RestController
@RequestMapping("/api/public/gardes")
public class PublicGardeController {

    private final GardeService gardeService;

    public PublicGardeController(GardeService gardeService) {
        this.gardeService = gardeService;
    }

    // GET /api/public/gardes?date=YYYY-MM-DD : si "date" est absent, on prend aujourd'hui
    @GetMapping
    public List<PharmacieDeGardeResponse> pharmaciesDeGarde(@RequestParam(required = false) LocalDate date) {
        LocalDate dateRecherchee = (date != null) ? date : LocalDate.now();
        return gardeService.calculerPharmaciesDeGarde(dateRecherchee);
    }
}