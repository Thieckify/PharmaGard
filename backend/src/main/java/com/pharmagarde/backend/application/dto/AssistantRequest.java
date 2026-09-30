package com.pharmagarde.backend.application.dto;

import jakarta.validation.constraints.NotBlank;

// Question posée à l'assistant par un visiteur de l'espace public
public record AssistantRequest(@NotBlank String question) {
}