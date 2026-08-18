package com.pharmagarde.backend.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requête de connexion (POST /api/auth/login).
 * Record utilisé car objet immuable, créé une fois par requête HTTP.
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}