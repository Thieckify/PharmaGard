package com.pharmagarde.backend.infrastructure.security;

import com.pharmagarde.backend.domain.port.out.EncodeurMotDePassePort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptateur du port EncodeurMotDePassePort, appuyé sur l'encodeur BCrypt de Spring Security.
 */
@Component
public class EncodeurMotDePasseAdapter implements EncodeurMotDePassePort {

    private final PasswordEncoder passwordEncoder;

    public EncodeurMotDePasseAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encoder(String motDePasse) {
        return passwordEncoder.encode(motDePasse);
    }
}
