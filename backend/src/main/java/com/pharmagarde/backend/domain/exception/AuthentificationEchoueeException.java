package com.pharmagarde.backend.domain.exception;

/**
 * Email ou mot de passe incorrect (traduite en 401, avec un message volontairement vague).
 */
public class AuthentificationEchoueeException extends ExceptionMetier {

    public AuthentificationEchoueeException(String message) {
        super(message);
    }
}
