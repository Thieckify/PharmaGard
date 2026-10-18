package com.pharmagarde.backend.domain.port.out;

import com.pharmagarde.backend.domain.model.User;

/**
 * Port de sortie : vérifie un couple email / mot de passe et renvoie l'utilisateur correspondant.
 * Lève AuthentificationEchoueeException si les identifiants sont incorrects.
 */
public interface AuthentificationPort {

    User authentifier(String email, String motDePasse);
}
