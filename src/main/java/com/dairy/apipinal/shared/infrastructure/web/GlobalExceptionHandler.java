package com.dairy.apipinal.shared.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(
            IllegalArgumentException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Requête invalide");
        problem.setDetail(exception.getMessage());

        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(
            IllegalStateException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problem.setTitle("Conflit métier");
        problem.setDetail(exception.getMessage());

        return problem;
    }
}
