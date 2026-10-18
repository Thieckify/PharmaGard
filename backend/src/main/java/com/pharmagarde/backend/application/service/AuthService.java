package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.LoginRequest;
import com.pharmagarde.backend.application.dto.LoginResponse;
import com.pharmagarde.backend.application.dto.RegisterRequest;
import com.pharmagarde.backend.domain.enums.Role;
import com.pharmagarde.backend.domain.exception.ConflitMetierException;
import com.pharmagarde.backend.domain.model.User;
import com.pharmagarde.backend.domain.port.out.AuthentificationPort;
import com.pharmagarde.backend.domain.port.out.EncodeurMotDePassePort;
import com.pharmagarde.backend.domain.port.out.GenerateurJetonPort;
import com.pharmagarde.backend.domain.port.out.UtilisateurRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service applicatif responsable de la connexion et de l'inscription self-service.
 * Ne connaît ni JWT, ni Spring Security, ni JPA : il passe par des ports (vérification des
 * identifiants, encodage du mot de passe, émission du jeton, stockage des comptes).
 */
@Service
public class AuthService {

    private final AuthentificationPort authentification;
    private final GenerateurJetonPort generateurJeton;
    private final UtilisateurRepositoryPort utilisateurs;
    private final EncodeurMotDePassePort encodeur;
    private final PharmacieService pharmacieService;

    public AuthService(AuthentificationPort authentification,
                       GenerateurJetonPort generateurJeton,
                       UtilisateurRepositoryPort utilisateurs,
                       EncodeurMotDePassePort encodeur,
                       PharmacieService pharmacieService) {
        this.authentification = authentification;
        this.generateurJeton = generateurJeton;
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
        this.pharmacieService = pharmacieService;
    }

    // Vérifie l'email et le mot de passe, puis émet un jeton. Un échec lève
    // AuthentificationEchoueeException (traduite en 401 par le gestionnaire global).
    public LoginResponse login(LoginRequest request) {
        User user = authentification.authentifier(request.email(), request.password());
        return construireReponse(user);
    }

    // Inscription self-service d'un pharmacien : crée le compte ET sa fiche pharmacie, puis
    // connecte directement l'utilisateur. Le rôle est toujours PHARMACIEN, jamais lu depuis la
    // requête. @Transactional : tout ou rien, jamais de compte orphelin sans fiche.
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (utilisateurs.existeParEmail(request.email())) {
            throw new ConflitMetierException("Un compte existe déjà avec cet email");
        }

        User nouveau = new User();
        nouveau.setEmail(request.email());
        nouveau.setPassword(encodeur.encoder(request.password()));
        nouveau.setRole(Role.PHARMACIEN);
        nouveau.setActive(true);
        // Pas de zone : seul un SUPERVISEUR de zone est rattaché à une zone

        // Premier enregistrement : c'est la base qui attribue l'id
        User cree = utilisateurs.enregistrer(nouveau);

        // Maintenant que l'id existe, on applique la règle métier "pharmacien = son propre tenant"
        cree.devenirSonPropreTenant();
        User enregistre = utilisateurs.enregistrer(cree);

        // Création de la fiche avec la même logique que POST /api/pharmacies (inactive par défaut)
        pharmacieService.upsertPharmacieDeUtilisateur(request.pharmacie(), enregistre.getId());

        return construireReponse(enregistre);
    }

    // Émet le jeton et assemble la réponse, commun au login et à l'inscription
    private LoginResponse construireReponse(User user) {
        return new LoginResponse(
                generateurJeton.generer(user),
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenantId(),
                user.getZoneId());
    }
}
