package com.pharmagarde.backend.domain.exception;

/**
 * L'action contredit une règle de gestion : chevauchement, garde déjà commencée, fiche non validée... (traduite en 409).
 */
public class ConflitMetierException extends ExceptionMetier {

    public ConflitMetierException(String message) {
        super(message);
    }
}
