package com.pharmagarde.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Question posée à l'assistant par un visiteur de l'espace public.
// Limite de longueur : une question démesurée n'a pas de sens pour cet usage et ralentirait
// le modèle (qui tourne sur CPU) inutilement.
public record AssistantRequest(
        @NotBlank
        @Size(max = 500, message = "La question ne doit pas dépasser 500 caractères")
        String question
) {
}