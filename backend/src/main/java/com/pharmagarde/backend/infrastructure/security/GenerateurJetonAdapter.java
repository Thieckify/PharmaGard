package com.pharmagarde.backend.infrastructure.security;

import com.pharmagarde.backend.domain.model.User;
import com.pharmagarde.backend.domain.port.out.GenerateurJetonPort;
import org.springframework.stereotype.Component;

/**
 * Adaptateur du port GenerateurJetonPort : émet un JWT signé via JwtTokenProvider.
 */
@Component
public class GenerateurJetonAdapter implements GenerateurJetonPort {

    private final JwtTokenProvider jwtTokenProvider;

    public GenerateurJetonAdapter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String generer(User user) {
        return jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole(), user.getTenantId(), user.getZoneId());
    }
}
