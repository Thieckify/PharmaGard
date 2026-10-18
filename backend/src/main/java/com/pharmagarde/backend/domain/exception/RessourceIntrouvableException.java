package com.pharmagarde.backend.domain.exception;

/**
 * La donnée demandée n'existe pas (traduite en 404).
 */
public class RessourceIntrouvableException extends ExceptionMetier {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
