package com.pharmagarde.backend.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuration CORS : autorise le frontend Angular (localhost:4200 en dev)
 * à appeler le backend depuis une origine différente.
 * Sans cette config, le navigateur bloque toutes les requêtes cross-origin
 * pour raisons de sécurité, ce qui empêche Angular de communiquer avec l'API.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées (Angular en dev). À adapter pour la prod.
        config.setAllowedOrigins(List.of("http://localhost:4200"));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Tous les headers sont autorisés (dont Authorization pour le JWT)
        config.setAllowedHeaders(List.of("*"));

        // Autorise l'envoi de credentials (utile pour les cookies, moins pour JWT header
        // mais bonne pratique de l'activer si un jour on ajoute des cookies)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}