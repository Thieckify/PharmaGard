package com.pharmagarde.backend.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuration CORS : autorise le frontend Angular à appeler le backend depuis une origine
 * différente. Sans cette config, le navigateur bloque toutes les requêtes cross-origin
 * pour raisons de sécurité, ce qui empêche Angular de communiquer avec l'API.
 * Les origines autorisées viennent de la configuration (app.cors.allowed-origins) : le code
 * ne change pas d'un environnement à l'autre.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(@Value("${app.cors.allowed-origins}") List<String> originsAutorisees) {

        // Garde-fous : une liste vide bloquerait silencieusement tous les appels du frontend, et un
        // joker "*" ouvrirait l'API à n'importe quel site (Spring l'interdit d'ailleurs avec
        // allowCredentials). On préfère échouer au démarrage, avec un message explicite.
        if (originsAutorisees.isEmpty()) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins est vide : indiquez au moins une origine (variable CORS_ALLOWED_ORIGINS)");
        }
        if (originsAutorisees.stream().anyMatch(origine -> origine.trim().equals("*"))) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins ne doit pas contenir '*' : listez les origines autorisées une par une");
        }

        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées : celles de la configuration (Angular en dev, le vrai site en prod)
        config.setAllowedOrigins(originsAutorisees);

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