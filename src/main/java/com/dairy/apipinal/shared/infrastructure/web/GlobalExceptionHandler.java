package com.dairy.apipinal.shared.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Contrat d'erreur unique de l'API : toute erreur est rendue au format ProblemDetail (RFC 9457),
 * avec un message lisible par l'utilisateur dans le champ {@code detail}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "Conflit métier", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        String detail = exception.getMessage() == null || exception.getMessage().equals("Access Denied")
                ? "Vous n'avez pas les droits nécessaires pour cette opération."
                : exception.getMessage();
        return problem(HttpStatus.FORBIDDEN, "Accès refusé", detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        if (detail.isBlank()) {
            detail = "Les données saisies sont invalides.";
        }
        return problem(HttpStatus.BAD_REQUEST, "Données invalides", detail);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Données invalides", "Les paramètres de la requête sont invalides.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Le contenu de la requête est illisible ou contient une valeur invalide.");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class})
    public ProblemDetail handleParameter(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide", "Un paramètre de la requête est absent ou invalide.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Violation de contrainte d'intégrité : {}", exception.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflit de données",
                "L'opération entre en conflit avec des données existantes (doublon ou référence invalide).");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException exception) {
        return problem(HttpStatus.CONFLICT, "Modification concurrente",
                "Les données ont été modifiées entre-temps. Rechargez puis réessayez.");
    }

    /**
     * Exceptions porteuses de leur propre ProblemDetail (ResponseStatusException, erreurs d'authentification…).
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(exception.getBody());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }
}
