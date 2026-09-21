package com.pharmagarde.backend.adapter.web.controller;

import com.pharmagarde.backend.application.dto.GardeRequest;
import com.pharmagarde.backend.application.dto.GardeResponse;
import com.pharmagarde.backend.application.service.GardeService;
import com.pharmagarde.backend.infrastructure.security.JwtUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST pour la gestion du planning de garde.
 * Toute la logique (validation, RG-03, propriété de la ressource) vit dans GardeService.
 */
@RestController
@RequestMapping("/api/gardes")
public class GardeController {

    private final GardeService gardeService;

    public GardeController(GardeService gardeService) {
        this.gardeService = gardeService;
    }

    // GET /api/gardes : liste les périodes déjà déclarées par la pharmacie du pharmacien connecté
    @GetMapping
    public List<GardeResponse> listMesGardes(@AuthenticationPrincipal JwtUserDetails principal) {
        return gardeService.listMesGardes(principal);
    }

    // POST /api/gardes : déclare une nouvelle période de garde
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GardeResponse declarerGarde(@Valid @RequestBody GardeRequest request,
                                        @AuthenticationPrincipal JwtUserDetails principal) {
        return gardeService.declarerGarde(request, principal);
    }

    // PUT /api/gardes/{id} : modifie une période de garde existante
    @PutMapping("/{id}")
    public GardeResponse modifierGarde(@PathVariable Long id,
                                        @Valid @RequestBody GardeRequest request,
                                        @AuthenticationPrincipal JwtUserDetails principal) {
        return gardeService.modifierGarde(id, request, principal);
    }

    // DELETE /api/gardes/{id} : supprime une période de garde
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerGarde(@PathVariable Long id,
                                @AuthenticationPrincipal JwtUserDetails principal) {
        gardeService.supprimerGarde(id, principal);
    }
}