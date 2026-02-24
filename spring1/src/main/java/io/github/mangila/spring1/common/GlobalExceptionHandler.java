package io.github.mangila.spring1.common;

import java.util.List;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  public ProblemDetail handleNoSuchElementException(NoSuchElementException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found");
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    List<String> errors =
        ex.getParameterValidationResults().stream()
            .map(
                result -> {
                  List<String> messages =
                      result.getResolvableErrors().stream()
                          .map(MessageSourceResolvable::getDefaultMessage)
                          .toList();
                  return result.getMethodParameter().getParameterName() + ": " + messages;
                })
            .toList();
    problemDetail.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problemDetail);
  }
}
