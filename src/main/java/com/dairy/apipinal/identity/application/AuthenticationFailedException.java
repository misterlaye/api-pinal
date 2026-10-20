package com.dairy.apipinal.identity.application;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Échec d'authentification (code OTP ou PIN incorrect, utilisateur inconnu).
 * Le message est volontairement identique quelle que soit la cause, pour ne pas révéler
 * l'existence d'un compte. Rendu en HTTP 401 au format ProblemDetail.
 */
public class AuthenticationFailedException extends ErrorResponseException {

    public AuthenticationFailedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, problem(detail), null);
    }

    private static ProblemDetail problem(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setTitle("Authentification refusée");
        return problem;
    }

    @Override
    public String getMessage() {
        return getBody().getDetail();
    }
}
