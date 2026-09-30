package com.pharmagarde.backend.adapter.web.controller;

import com.pharmagarde.backend.application.dto.PharmaciePubliqueResponse;
import com.pharmagarde.backend.application.service.PharmacieRechercheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint public (sans authentification, RG-04) pour rechercher des pharmacies,
 * éventuellement par quartier. Toute la logique (RG-05 inclus) vit dans PharmacieRechercheService.
 */
@RestController
@RequestMapping("/api/public/pharmacies")
public class PublicPharmacieController {

    private final PharmacieRechercheService rechercheService;

    public PublicPharmacieController(PharmacieRechercheService rechercheService) {
        this.rechercheService = rechercheService;
    }

    // GET /api/public/pharmacies?quartier=8 : si "quartier" est absent, on renvoie toutes les pharmacies actives
    @GetMapping
    public List<PharmaciePubliqueResponse> rechercher(@RequestParam(required = false) Long quartier) {
        return rechercheService.rechercher(quartier);
    }
}