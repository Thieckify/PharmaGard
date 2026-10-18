package com.pharmagarde.backend.domain.exception;

/**
 * Un service externe nécessaire ne répond pas, par exemple le modèle de langage (traduite en 503).
 */
public class ServiceIndisponibleException extends ExceptionMetier {

    public ServiceIndisponibleException(String message) {
        super(message);
    }
}
