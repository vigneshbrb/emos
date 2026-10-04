package com.emos.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(CaseNotFoundException.class)
    ProblemDetail notFound(CaseNotFoundException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Operational case not found");
        detail.setType(URI.create("https://emos.local/problems/case-not-found"));
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(IllegalArgumentException exception) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        detail.setTitle("Invalid request");
        detail.setType(URI.create("https://emos.local/problems/invalid-request"));
        return detail;
    }
}

final class CaseNotFoundException extends RuntimeException {
    CaseNotFoundException(UUID id) { super("Operational case does not exist: " + id); }
}
