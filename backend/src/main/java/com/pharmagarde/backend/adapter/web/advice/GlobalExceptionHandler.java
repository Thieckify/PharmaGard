package com.pharmagarde.backend.adapter.web.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Intercepte les exceptions levées par les controllers/services et construit directement
 * une ResponseEntity, sans jamais appeler response.sendError(). C'est ce point précis qui
 * évite le second passage dans la chaîne de filtres Spring Security déclenché par le
 * mécanisme de page d'erreur du conteneur servlet (dispatch interne vers /error) — le piège
 * qui masquait nos vrais codes d'erreur (403, 404...) derrière un 401 générique.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Échec de login (mauvais email/mot de passe) -> 401, avec un message volontairement
    // vague : on ne précise jamais si c'est l'email ou le mot de passe qui est faux,
    // pour ne pas révéler à un attaquant qu'un email existe dans la base.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> gererAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(corpsErreur(HttpStatus.UNAUTHORIZED.value(), "Email ou mot de passe incorrect"));
    }

    // Exceptions métier levées volontairement avec un code HTTP précis
    // (ex: 403 dans PharmacieService.validerPharmacie). On respecte ce code tel quel.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> gererResponseStatusException(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(status).body(corpsErreur(status, message));
    }

    // Ressource introuvable (tous les orElseThrow(...) des services) -> 404
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> gererIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(corpsErreur(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    // Erreurs de validation @Valid (ex: le champ "valide" manquant dans ValidationRequest) -> 400,
    // avec le détail champ par champ. Déclarée explicitement pour ne pas tomber dans le
    // gestionnaire générique ci-dessous, qui la renverrait en 500 sans ce détail utile.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> gererValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> erreursParChamp = new LinkedHashMap<>();
        for (FieldError erreur : ex.getBindingResult().getFieldErrors()) {
            erreursParChamp.put(erreur.getField(), erreur.getDefaultMessage());
        }
        Map<String, Object> corps = corpsErreur(HttpStatus.BAD_REQUEST.value(), "Requête invalide");
        corps.put("erreurs", erreursParChamp);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corps);
    }

    // Filet de sécurité : toute exception vraiment inattendue (bug) -> 500 explicite,
    // plutôt que de la laisser remonter au conteneur et se faire masquer en 401.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> gererExceptionGenerique(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(corpsErreur(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne du serveur"));
    }

    // Construit un corps de réponse JSON cohérent, dans le même format que celui déjà
    // utilisé par JwtAuthenticationEntryPoint (status/error/message).
    private Map<String, Object> corpsErreur(int status, String message) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("status", status);
        corps.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        corps.put("message", message);
        return corps;
    }
}