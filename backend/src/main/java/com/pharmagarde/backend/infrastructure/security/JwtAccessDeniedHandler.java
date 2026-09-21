package com.pharmagarde.backend.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Déclenché quand un utilisateur AUTHENTIFIÉ (badge valide) tente d'accéder à une route
 * réservée à un autre rôle (ex: PHARMACIEN sur une route SUPERVISEUR).
 * Écrit la réponse 403 directement, sans jamais passer par response.sendError() —
 * exactement pour éviter le détour qui, sinon, écraserait ce 403 par un 401.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String json = """
                {"status":403,"error":"Forbidden","message":"Vous n'avez pas les droits pour effectuer cette action"}
                """;

        response.getWriter().write(json);
    }
}