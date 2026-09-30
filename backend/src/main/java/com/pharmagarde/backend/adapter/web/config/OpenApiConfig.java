package com.pharmagarde.backend.adapter.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentation de l'API (Swagger UI) : description générale, schéma d'authentification par
 * jeton JWT (bouton "Authorize"), et cadenas uniquement sur les routes qui exigent un jeton.
 * Cette configuration ne fait que décrire l'API : elle n'a aucun effet sur la sécurité réelle,
 * qui reste définie dans SecurityConfig.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEMA_JWT = "jeton-jwt";

    // Routes qui n'exigent pas de jeton (cf. SecurityConfig) : elles n'ont pas de cadenas.
    // /api/assistant est déjà listée pour la séance à venir (assistant Ollama, public).
    private static final List<String> PREFIXES_PUBLICS =
            List.of("/api/public/", "/api/auth/", "/api/assistant");

    @Bean
    public OpenAPI pharmaGardeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PharmaGarde API")
                        .version("1.0.0")
                        .description("""
                                API de la plateforme PharmaGarde : pharmacies de garde à Dakar.

                                Pour essayer les routes protégées : appelez POST /api/auth/login avec un compte, \
                                copiez le jeton renvoyé, cliquez sur le bouton Authorize, puis collez le jeton \
                                (sans le mot Bearer)."""))
                .components(new Components().addSecuritySchemes(SCHEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton JWT obtenu avec POST /api/auth/login")));
    }

    // Ajoute l'exigence du jeton à chaque route qui n'est pas publique
    @Bean
    public OpenApiCustomizer exigerLeJetonSurLesRoutesProtegees() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((chemin, route) -> {
                boolean publique = PREFIXES_PUBLICS.stream().anyMatch(chemin::startsWith);
                if (!publique) {
                    route.readOperations().forEach(operation ->
                            operation.addSecurityItem(new SecurityRequirement().addList(SCHEMA_JWT)));
                }
            });
        };
    }
}