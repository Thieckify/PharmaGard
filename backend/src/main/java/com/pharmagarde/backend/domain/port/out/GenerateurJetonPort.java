package com.pharmagarde.backend.domain.port.out;

import com.pharmagarde.backend.domain.model.User;

/**
 * Port de sortie : émet le jeton de connexion qui porte l'identité et le périmètre de l'utilisateur.
 */
public interface GenerateurJetonPort {

    String generer(User user);
}
