package com.accountabilityatlas.userservice.exception;

import com.accountabilityatlas.userservice.web.model.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<Error> handleEmailExists(EmailAlreadyExistsException ex) {
    Error error = new Error();
    error.setCode("EMAIL_EXISTS");
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<Error> handleInvalidCredentials(InvalidCredentialsException ex) {
    Error error = new Error();
    error.setCode("INVALID_CREDENTIALS");
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<Error> handleAccountLocked(AccountLockedException ex) {
    Error error = new Error();
    error.setCode("ACCOUNT_LOCKED");
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Error> handleValidation(MethodArgumentNotValidException ex) {
    Error error = new Error();
    error.setCode("VALIDATION_ERROR");
    error.setMessage("Request validation failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Error> handleUserNotFound(UserNotFoundException ex) {
    Error error = new Error();
    error.setCode("USER_NOT_FOUND");
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }
}
