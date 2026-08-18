package com.pharmagarde.backend.infrastructure.security;

import com.pharmagarde.backend.infrastructure.persistence.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapte UserEntity au contrat UserDetails attendu par Spring Security.
 * Sert de pont entre notre modèle de données et le mécanisme d'authentification.
 */
public class UserPrincipal implements UserDetails {

    private final UserEntity user;

    public UserPrincipal(UserEntity user) {
        this.user = user;
    }

    // Expose l'entité complète pour que le controller puisse générer le JWT
    // avec toutes les infos (id, tenantId, zoneId)
    public UserEntity getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security attend les rôles préfixés par "ROLE_"
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // déjà hashé en BCrypt en base
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // l'email sert d'identifiant de connexion
    }

    @Override
    public boolean isEnabled() {
        return user.isActive(); // un compte désactivé ne peut pas se connecter
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}