package com.pharmagarde.backend.adapter.web.advice;

import com.pharmagarde.backend.domain.exception.AccesRefuseException;
import com.pharmagarde.backend.domain.exception.AuthentificationEchoueeException;
import com.pharmagarde.backend.domain.exception.ConflitMetierException;
import com.pharmagarde.backend.domain.exception.DonneeInvalideException;
import com.pharmagarde.backend.domain.exception.ExceptionMetier;
import com.pharmagarde.backend.domain.exception.RessourceIntrouvableException;
import com.pharmagarde.backend.domain.exception.ServiceIndisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Échec de login (mauvais email/mot de passe) -> 401, avec un message volontairement
    // vague : on ne précise jamais si c'est l'email ou le mot de passe qui est faux,
    // pour ne pas révéler à un attaquant qu'un email existe dans la base.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> gererAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(corpsErreur(HttpStatus.UNAUTHORIZED.value(), "Email ou mot de passe incorrect"));
    }

    // Erreurs métier du domaine : le domaine ne connaît pas HTTP, c'est ici, dans la couche
    // adapter, que chaque sorte d'erreur reçoit son code. Le message, rédigé par le service
    // pour l'utilisateur, est renvoyé tel quel.
    @ExceptionHandler(ExceptionMetier.class)
    public ResponseEntity<Map<String, Object>> gererExceptionMetier(ExceptionMetier ex) {
        HttpStatus statut = switch (ex) {
            case RessourceIntrouvableException e -> HttpStatus.NOT_FOUND;
            case AccesRefuseException e -> HttpStatus.FORBIDDEN;
            case ConflitMetierException e -> HttpStatus.CONFLICT;
            case DonneeInvalideException e -> HttpStatus.BAD_REQUEST;
            case AuthentificationEchoueeException e -> HttpStatus.UNAUTHORIZED;
            case ServiceIndisponibleException e -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return reponse(statut, ex.getMessage());
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

    // Corps de requête illisible : JSON mal formé, ou valeur d'un mauvais type (un texte à la place
    // d'un nombre...) -> 400. Cette exception ne porte pas de code HTTP : il faut la traiter ici.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> gererCorpsIllisible(HttpMessageNotReadableException ex) {
        return reponse(HttpStatus.BAD_REQUEST, "Corps de la requête illisible ou mal formé");
    }

    // Paramètre d'URL d'un mauvais type (date=abc, identifiant non numérique...) -> 400.
    // Même remarque : cette exception ne porte pas de code HTTP.
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> gererParametreInvalide(TypeMismatchException ex) {
        return reponse(HttpStatus.BAD_REQUEST, "Valeur invalide pour un paramètre de la requête");
    }

    // Filet de sécurité, en deux temps.
    // 1) Les erreurs HTTP levées par Spring lui-même avant d'atteindre un controller : URL
    //    inexistante (404), méthode non autorisée (405), type de contenu non pris en charge (415)...
    //    Elles portent déjà leur code (interface ErrorResponse) : on le respecte, au lieu de les
    //    transformer en 500 alors que c'est l'appelant qui s'est trompé.
    // 2) Tout le reste est un vrai bug : 500 explicite, et surtout journalisé. Sans cette ligne de
    //    log, une vraie panne ne laisserait aucune trace dans la console.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> gererExceptionGenerique(Exception ex) {
        if (ex instanceof ErrorResponse erreurHttp && erreurHttp.getStatusCode().is4xxClientError()) {
            int status = erreurHttp.getStatusCode().value();
            return ResponseEntity.status(status)
                    .headers(erreurHttp.getHeaders())
                    .body(corpsErreur(status, messagePourStatut(status)));
        }

        log.error("Erreur inattendue", ex);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
    }

    // Message en français pour les erreurs HTTP de Spring (dont les textes d'origine sont en anglais)
    private String messagePourStatut(int status) {
        return switch (status) {
            case 400 -> "Requête invalide";
            case 404 -> "Ressource introuvable";
            case 405 -> "Méthode HTTP non autorisée sur cette route";
            case 406 -> "Format de réponse non disponible";
            case 415 -> "Type de contenu non pris en charge";
            default -> "Requête refusée";
        };
    }

    private ResponseEntity<Map<String, Object>> reponse(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(corpsErreur(statut.value(), message));
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