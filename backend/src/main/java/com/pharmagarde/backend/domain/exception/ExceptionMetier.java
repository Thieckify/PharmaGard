package com.pharmagarde.backend.domain.exception;

/**
 * Mère de toutes les erreurs métier levées par la couche application. Le domaine ne connaît
 * pas HTTP : c'est le gestionnaire global d'exceptions, dans la couche adapter, qui traduit
 * chaque sorte d'erreur en code HTTP.
 */
public abstract class ExceptionMetier extends RuntimeException {

    protected ExceptionMetier(String message) {
        super(message);
    }
}
