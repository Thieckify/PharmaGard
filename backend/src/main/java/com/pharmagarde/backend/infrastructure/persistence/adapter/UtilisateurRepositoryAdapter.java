package com.pharmagarde.backend.infrastructure.persistence.adapter;

import com.pharmagarde.backend.domain.model.User;
import com.pharmagarde.backend.domain.port.out.UtilisateurRepositoryPort;
import com.pharmagarde.backend.infrastructure.persistence.entity.ZoneEntity;
import com.pharmagarde.backend.infrastructure.persistence.mapper.UserMapper;
import com.pharmagarde.backend.infrastructure.persistence.repository.UserRepository;
import com.pharmagarde.backend.infrastructure.persistence.repository.ZoneRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptateur JPA du port UtilisateurRepositoryPort.
 */
@Component
public class UtilisateurRepositoryAdapter implements UtilisateurRepositoryPort {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;

    public UtilisateurRepositoryAdapter(UserRepository userRepository, ZoneRepository zoneRepository) {
        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
    }

    @Override
    public Optional<User> trouverParId(Long id) {
        return userRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public boolean existeParEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User enregistrer(User user) {
        ZoneEntity zone = user.getZoneId() != null ? zoneRepository.findById(user.getZoneId()).orElse(null) : null;
        return UserMapper.toDomain(userRepository.save(UserMapper.toEntity(user, zone)));
    }

    @Override
    public void supprimer(Long id) {
        userRepository.deleteById(id);
    }
}
