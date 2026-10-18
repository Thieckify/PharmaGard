package com.pharmagarde.backend.domain.exception;

/**
 * L'utilisateur est connecté mais n'a pas le droit d'agir sur cette donnée, hors de son tenant ou de sa zone (traduite en 403).
 */
public class AccesRefuseException extends ExceptionMetier {

    public AccesRefuseException(String message) {
        super(message);
    }
}
