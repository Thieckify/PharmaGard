package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.LoginRequest;
import com.pharmagarde.backend.application.dto.LoginResponse;
import com.pharmagarde.backend.infrastructure.security.JwtTokenProvider;
import com.pharmagarde.backend.infrastructure.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Service applicatif responsable de la logique d'authentification.
 * Contient toute la logique métier du login : vérification des identifiants
 * et construction de la réponse. Le controller ne fait que transporter
 * la requête HTTP vers ce service.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authentifie un utilisateur à partir de son email et mot de passe,
     * puis génère un token JWT s'il est valide.
     * Lève une exception (gérée par Spring) si les identifiants sont incorrects.
     */
    public LoginResponse login(LoginRequest request) {
        // Délègue la vérification email/mot de passe à Spring Security
        // (utilise CustomUserDetailsService + BCryptPasswordEncoder en coulisses)
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var principal = (UserPrincipal) authentication.getPrincipal();
        var user = principal.getUser();

        // Extraction du zoneId : peut être null (ADMIN, SUPERVISEUR général)
        Long zoneId = user.getZone() != null ? user.getZone().getId() : null;

        // Génération du token avec toutes les infos de portée (role, tenantId, zoneId)
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole(), user.getTenantId(), zoneId
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenantId(),
                zoneId
        );
    }
}