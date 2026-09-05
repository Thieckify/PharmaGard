package com.pharmagarde.backend.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration centrale de Spring Security.
 * Définit :
 * - les routes accessibles sans authentification (login, endpoints publics)
 * - les routes qui exigent un token JWT valide
 * - le mode "stateless" (pas de session HTTP, tout repose sur le JWT)
 * - l'insertion de notre filtre JWT dans la chaîne de filtres Spring
 * - le renvoi explicite de 401 (au lieu du 403 par défaut) pour une requête non authentifiée
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * Bean utilisé pour hasher les mots de passe (BCrypt),
     * et pour vérifier un mot de passe en clair contre son hash lors du login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose l'AuthenticationManager de Spring pour que AuthController
     * puisse l'utiliser afin de vérifier email + mot de passe au login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS : autorise le frontend Angular (localhost:4200 en dev)
                .cors(cors -> {})

                // Désactivation CSRF : inutile en API stateless authentifiée par JWT
                // (CSRF protège les sessions basées sur cookies, pas notre cas)
                .csrf(csrf -> csrf.disable())

                // Pas de session HTTP créée : chaque requête s'authentifie via son propre token
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Authentification (login) : toujours public
                        .requestMatchers("/api/auth/**").permitAll()

                        // Endpoints publics métier (recherche de pharmacies de garde, etc.)
                        .requestMatchers("/api/public/**").permitAll()

                        // Documentation Swagger (si ajoutée plus tard)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Gestion des fiches pharmacie : réservé aux pharmaciens (upsert de leur fiche)
                        // et superviseurs (consultation), pas à un ADMIN qui n'a pas ce rôle métier
                        .requestMatchers("/api/pharmacies/**").hasAnyRole("PHARMACIEN", "SUPERVISEUR")

                        // Tout le reste exige un token JWT valide
                        .anyRequest().authenticated()
                )

                // Force le renvoi de 401 (au lieu du 403 par défaut) quand
                // une requête non authentifiée tente d'accéder à une route protégée
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // Insertion de notre filtre JWT avant le filtre standard de Spring Security,
                // pour que l'authentification soit posée dans le contexte AVANT
                // que Spring vérifie les autorisations
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}