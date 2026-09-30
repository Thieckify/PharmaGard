package com.pharmagarde.backend.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 * - le renvoi explicite de 401 (utilisateur non connecté) et 403 (connecté mais rôle refusé),
 *   tous deux écrits directement pour éviter le détour par /error qui les écraserait
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                           JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
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

                        // Documentation Swagger : /swagger-ui.html est l'adresse d'entrée (elle redirige
                        // vers /swagger-ui/index.html), que le motif /swagger-ui/** ne couvre pas.
                        // En production, springdoc est désactivé : ces routes n'existent alors plus.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Validation d'une pharmacie : réservé au SUPERVISEUR uniquement (séparation
                        // des devoirs, cf. RG-11). Règle SPÉCIFIQUE déclarée AVANT la règle générale
                        // /api/pharmacies/** ci-dessous : Spring Security retient la première règle
                        // qui correspond à l'URL, donc l'ordre ici n'est pas cosmétique.
                        .requestMatchers(HttpMethod.PATCH, "/api/pharmacies/*/validation").hasRole("SUPERVISEUR")

                        // Suppression d'une pharmacie (RG-15) : réservée au SUPERVISEUR. Même remarque
                        // sur l'ordre : sans cette règle placée avant la règle générale, un PHARMACIEN
                        // pourrait supprimer une pharmacie, puisque la règle générale l'autorise.
                        .requestMatchers(HttpMethod.DELETE, "/api/pharmacies/*").hasRole("SUPERVISEUR")

                        // Gestion des fiches pharmacie : réservé aux pharmaciens (upsert de leur fiche)
                        // et superviseurs (consultation), pas à un ADMIN qui n'a pas ce rôle métier
                        .requestMatchers("/api/pharmacies/**").hasAnyRole("PHARMACIEN", "SUPERVISEUR")

                        // Déclaration d'une période de garde : réservé au PHARMACIEN uniquement —
                        // un SUPERVISEUR n'a pas à déclarer le planning à la place du pharmacien
                        .requestMatchers(HttpMethod.POST, "/api/gardes").hasRole("PHARMACIEN")

                        // Consultation de son propre planning : même règle de rôle que la déclaration
                        .requestMatchers(HttpMethod.GET, "/api/gardes").hasRole("PHARMACIEN")

                        // Modification/suppression d'une période de garde : même règle de rôle ;
                        // la vérification que la garde appartient bien à SA pharmacie se fait
                        // dans GardeService (trouverGardePropre), pas ici — SecurityConfig ne
                        // connaît que les rôles, jamais la propriété d'une ressource précise.
                        .requestMatchers(HttpMethod.PUT, "/api/gardes/*").hasRole("PHARMACIEN")
                        .requestMatchers(HttpMethod.DELETE, "/api/gardes/*").hasRole("PHARMACIEN")

                        // Tout le reste exige un token JWT valide
                        .anyRequest().authenticated()
                )

                // Deux cas distincts : "pas connecté du tout" (401) vs "connecté mais rôle refusé" (403).
                // Les deux écrivent leur réponse directement, sans jamais passer par sendError().
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // Insertion de notre filtre JWT avant le filtre standard de Spring Security,
                // pour que l'authentification soit posée dans le contexte AVANT
                // que Spring vérifie les autorisations
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}