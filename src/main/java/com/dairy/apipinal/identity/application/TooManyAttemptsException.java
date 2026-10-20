package com.dairy.apipinal.identity.application;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Trop de tentatives d'authentification : le code (OTP) ou le compte (PIN) est verrouillé.
 * Rendu en HTTP 429 au format ProblemDetail.
 */
public class TooManyAttemptsException extends ErrorResponseException {

    public TooManyAttemptsException(String detail) {
        super(HttpStatus.TOO_MANY_REQUESTS, problem(detail), null);
    }

    private static ProblemDetail problem(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detail);
        problem.setTitle("Trop de tentatives");
        return problem;
    }

    @Override
    public String getMessage() {
        return getBody().getDetail();
    }
}
