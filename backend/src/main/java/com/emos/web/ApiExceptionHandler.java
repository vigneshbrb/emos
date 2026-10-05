package com.emos.web;

import com.emos.improvementknowledge.application.ImprovementValidationException;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

  @ExceptionHandler(ImprovementValidationException.class)
  ProblemDetail unprocessable(ImprovementValidationException exception) {
    var detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    detail.setTitle("Improvement workflow validation failed");
    detail.setType(URI.create("https://emos.local/problems/improvement-validation"));
    return detail;
  }
}

final class CaseNotFoundException extends RuntimeException {
  CaseNotFoundException(UUID id) {
    super("Operational case does not exist: " + id);
  }
}
