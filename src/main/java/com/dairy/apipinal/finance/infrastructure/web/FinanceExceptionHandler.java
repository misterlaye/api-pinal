package com.dairy.apipinal.finance.infrastructure.web;

import com.dairy.apipinal.finance.application.InvalidRentabilityPeriodException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice(basePackageClasses = FinanceController.class)
public class FinanceExceptionHandler {

    @ExceptionHandler(InvalidRentabilityPeriodException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRentabilityPeriod(
            InvalidRentabilityPeriodException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}