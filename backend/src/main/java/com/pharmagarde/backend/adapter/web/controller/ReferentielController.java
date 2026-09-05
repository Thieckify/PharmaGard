package com.pharmagarde.backend.adapter.web.controller;

import com.pharmagarde.backend.application.dto.QuartierResponse;
import com.pharmagarde.backend.application.service.ReferentielService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints publics pour consulter les référentiels (zones, quartiers).
 */
@RestController
@RequestMapping("/api/public")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    // GET /api/public/quartiers : liste tous les quartiers, pour peupler menus déroulants et recherche publique
    @GetMapping("/quartiers")
    public List<QuartierResponse> listQuartiers() {
        return referentielService.listQuartiers();
    }
}