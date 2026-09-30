package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.LoginRequest;
import com.pharmagarde.backend.application.dto.LoginResponse;
import com.pharmagarde.backend.application.dto.RegisterRequest;
import com.pharmagarde.backend.domain.enums.Role;
import com.pharmagarde.backend.domain.model.User;
import com.pharmagarde.backend.infrastructure.persistence.mapper.UserMapper;
import com.pharmagarde.backend.infrastructure.persistence.repository.UserRepository;
import com.pharmagarde.backend.infrastructure.security.JwtTokenProvider;
import com.pharmagarde.backend.infrastructure.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service applicatif responsable de la logique d'authentification.
 * Contient toute la logique métier du login et de l'inscription self-service.
 * Travaille uniquement avec le modèle métier User (jamais avec l'entité JPA),
 * la conversion étant faite par UserMapper. Le controller ne fait que transporter
 * la requête HTTP vers ce service.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PharmacieService pharmacieService;

    public AuthService(AuthenticationManager authenticationManager,
                        JwtTokenProvider jwtTokenProvider,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        PharmacieService pharmacieService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pharmacieService = pharmacieService;
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

        // On convertit tout de suite en modèle métier : la suite ne connaît plus l'entité JPA
        User user = UserMapper.toDomain(principal.getUser());
        return construireReponse(user);
    }

    /**
     * Inscription self-service d'un pharmacien : crée le compte ET sa fiche pharmacie,
     * puis connecte directement l'utilisateur (renvoie un JWT).
     * Le rôle est toujours PHARMACIEN ici, jamais lu depuis la requête.
     * @Transactional : tout ou rien. Si la création de la fiche échoue (quartier inconnu...),
     * la création du compte est annulée aussi, on ne laisse jamais de compte orphelin.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }

        User nouveau = new User();
        nouveau.setEmail(request.email());
        nouveau.setPassword(passwordEncoder.encode(request.password()));
        nouveau.setRole(Role.PHARMACIEN);
        nouveau.setActive(true);
        // Pas de zone : seul un SUPERVISEUR de zone est rattaché à une zone

        // Premier enregistrement : c'est la base qui attribue l'id (auto-incrément)
        User cree = UserMapper.toDomain(userRepository.save(UserMapper.toEntity(nouveau, null)));

        // Maintenant que l'id existe, on applique la règle métier "pharmacien = son propre tenant"
        cree.devenirSonPropreTenant();
        User enregistre = UserMapper.toDomain(userRepository.save(UserMapper.toEntity(cree, null)));

        // Création de la fiche pharmacie avec la même logique que POST /api/pharmacies
        // (fiche inactive par défaut, en attente de validation par un superviseur)
        pharmacieService.upsertPharmacieDeUtilisateur(request.pharmacie(), enregistre.getId());

        return construireReponse(enregistre);
    }

    // Génère le token JWT et assemble la réponse, commun au login et à l'inscription
    private LoginResponse construireReponse(User user) {
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole(), user.getTenantId(), user.getZoneId()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenantId(),
                user.getZoneId()
        );
    }
}