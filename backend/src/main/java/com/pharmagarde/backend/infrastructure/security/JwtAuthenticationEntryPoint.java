package com.pharmagarde.backend.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Point d'entrée déclenché quand une requête non authentifiée
 * tente d'accéder à une route protégée.
 * Renvoie explicitement 401 (et non le 403 par défaut de Spring),
 * pour que le frontend distingue "pas connecté" de "pas autorisé".
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // JSON écrit manuellement : pas besoin d'ObjectMapper pour un objet aussi simple,
        // et on évite d'avoir à gérer les exceptions de sérialisation.
        String json = """
                {"status":401,"error":"Unauthorized","message":"Authentification requise ou token invalide"}
                """;

        response.getWriter().write(json);
    }
}