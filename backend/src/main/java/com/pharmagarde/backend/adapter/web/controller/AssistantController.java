package com.pharmagarde.backend.adapter.web.controller;

import com.pharmagarde.backend.application.dto.AssistantRequest;
import com.pharmagarde.backend.application.dto.AssistantResponse;
import com.pharmagarde.backend.application.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// POST /api/assistant : endpoint public de l'assistant conversationnel (aide à trouver une
// pharmacie de garde). Pas d'authentification : accessible à tout visiteur de l'espace public.
// produces déclare explicitement le charset UTF-8 : sans lui, Spring écrit la réponse en UTF-8
// mais omet le paramètre charset de l'en-tête Content-Type, et certains clients (dont
// PowerShell) retombent alors sur ISO-8859-1 pour la lecture, ce qui casse les accents.
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public AssistantResponse poserQuestion(@Valid @RequestBody AssistantRequest request) {
        String reponse = assistantService.repondre(request.question());
        return new AssistantResponse(reponse);
    }
}