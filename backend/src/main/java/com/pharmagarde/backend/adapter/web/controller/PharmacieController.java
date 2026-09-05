package com.pharmagarde.backend.adapter.web.controller;

import com.pharmagarde.backend.application.dto.PharmacieRequest;
import com.pharmagarde.backend.application.dto.PharmacieResponse;
import com.pharmagarde.backend.application.service.PharmacieService;
import com.pharmagarde.backend.infrastructure.security.JwtUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST pour la gestion des fiches pharmacie.
 * Toute la logique (résolution du quartier, upsert, filtrage par rôle) vit dans PharmacieService.
 */
@RestController
@RequestMapping("/api/pharmacies")
public class PharmacieController {

    private final PharmacieService pharmacieService;

    public PharmacieController(PharmacieService pharmacieService) {
        this.pharmacieService = pharmacieService;
    }

    // POST /api/pharmacies : crée ou met à jour la fiche du pharmacien connecté
    @PostMapping
    public PharmacieResponse upsertPharmacie(@Valid @RequestBody PharmacieRequest request,
                                              @AuthenticationPrincipal JwtUserDetails principal) {
        return pharmacieService.upsertPharmacie(request, principal);
    }

    // GET /api/pharmacies : liste scopée selon le rôle (voir PharmacieService.listPharmacies)
    @GetMapping
    public List<PharmacieResponse> listPharmacies(@AuthenticationPrincipal JwtUserDetails principal) {
        return pharmacieService.listPharmacies(principal);
    }
}