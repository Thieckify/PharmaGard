package com.pharmagarde.backend.domain.port.out;

import com.pharmagarde.backend.domain.model.User;

import java.util.Optional;

/**
 * Port de sortie pour les comptes utilisateurs.
 */
public interface UtilisateurRepositoryPort {

    Optional<User> trouverParId(Long id);

    boolean existeParEmail(String email);

    User enregistrer(User user);

    void supprimer(Long id);
}
