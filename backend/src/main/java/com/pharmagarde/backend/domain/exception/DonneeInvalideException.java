package com.pharmagarde.backend.domain.exception;

/**
 * Une donnée fournie est incorrecte : dates incohérentes, champ obligatoire manquant... (traduite en 400).
 */
public class DonneeInvalideException extends ExceptionMetier {

    public DonneeInvalideException(String message) {
        super(message);
    }
}
