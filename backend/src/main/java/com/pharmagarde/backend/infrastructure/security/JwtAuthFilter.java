package com.pharmagarde.backend.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre exécuté une seule fois par requête (OncePerRequestFilter).
 * Il lit l'en-tête "Authorization: Bearer <token>", vérifie le JWT,
 * puis place l'utilisateur authentifié dans le SecurityContext de Spring
 * pour que le reste de la chaîne (controllers, @PreAuthorize, etc.)
 * sache "qui" fait la requête et avec quel rôle.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Pas de header, ou header mal formé -> on laisse passer sans authentifier
        // Le SecurityConfig décidera ensuite si la route exige un token ou non.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraction du token (on retire le préfixe "Bearer ")
        String token = authHeader.substring(7);

        if (jwtTokenProvider.isValid(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);

            // Spring Security attend les rôles préfixés par "ROLE_"
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var authentication = new UsernamePasswordAuthenticationToken(
                    email,          // principal (identifiant de l'utilisateur)
                    null,           // credentials (pas nécessaire, déjà authentifié via le token)
                    authorities     // rôles/permissions
            );

            // Attache manuellement les infos utiles (tenantId, zoneId, userId)
            // pour que les controllers puissent les récupérer via la requête si besoin
            authentication.setDetails(new JwtUserDetails(
                    jwtTokenProvider.getUserIdFromToken(token),
                    jwtTokenProvider.getTenantIdFromToken(token),
                    jwtTokenProvider.getZoneIdFromToken(token)
            ));

            // On enregistre l'authentification dans le contexte de sécurité de la requête
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Le token est invalide ou absent : on ne bloque pas ici,
        // c'est SecurityConfig qui décidera si la route exige une authentification
        filterChain.doFilter(request, response);
    }
}