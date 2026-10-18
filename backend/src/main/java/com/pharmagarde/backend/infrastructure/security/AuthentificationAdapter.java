package com.pharmagarde.backend.infrastructure.security;

import com.pharmagarde.backend.domain.exception.AuthentificationEchoueeException;
import com.pharmagarde.backend.domain.model.User;
import com.pharmagarde.backend.domain.port.out.AuthentificationPort;
import com.pharmagarde.backend.infrastructure.persistence.mapper.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Adaptateur Spring Security du port AuthentificationPort : délègue la vérification de l'email
 * et du mot de passe (BCrypt) à l'AuthenticationManager, et traduit l'échec en erreur métier.
 */
@Component
public class AuthentificationAdapter implements AuthentificationPort {

    private final AuthenticationManager authenticationManager;

    public AuthentificationAdapter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public User authentifier(String email, String motDePasse) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, motDePasse));
            var principal = (UserPrincipal) authentication.getPrincipal();
            return UserMapper.toDomain(principal.getUser());
        } catch (AuthenticationException e) {
            // Message volontairement vague : on ne dit jamais si c'est l'email ou le mot de passe
            throw new AuthentificationEchoueeException("Email ou mot de passe incorrect");
        }
    }
}
