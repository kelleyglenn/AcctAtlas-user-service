package com.accountabilityatlas.userservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountabilityatlas.userservice.web.model.Error;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void handleEmailExists_returns409WithEmailExistsCode() {
    // Arrange
    EmailAlreadyExistsException ex = new EmailAlreadyExistsException();

    // Act
    ResponseEntity<Error> response = handler.handleEmailExists(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("EMAIL_EXISTS");
    assertThat(response.getBody().getMessage())
        .isEqualTo("An account with this email already exists");
  }

  @Test
  void handleInvalidCredentials_returns401WithInvalidCredentialsCode() {
    // Arrange
    InvalidCredentialsException ex = new InvalidCredentialsException();

    // Act
    ResponseEntity<Error> response = handler.handleInvalidCredentials(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_CREDENTIALS");
    assertThat(response.getBody().getMessage()).isEqualTo("Email or password is incorrect");
  }

  @Test
  void handleInvalidRefreshToken_returns401WithInvalidRefreshTokenCode() {
    // Arrange
    InvalidRefreshTokenException ex =
        new InvalidRefreshTokenException("Invalid or expired refresh token");

    // Act
    ResponseEntity<Error> response = handler.handleInvalidRefreshToken(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("INVALID_REFRESH_TOKEN");
    assertThat(response.getBody().getMessage()).isEqualTo("Invalid or expired refresh token");
  }

  @Test
  void handleAccountLocked_returns429WithAccountLockedCode() {
    // Arrange
    AccountLockedException ex = new AccountLockedException();

    // Act
    ResponseEntity<Error> response = handler.handleAccountLocked(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("ACCOUNT_LOCKED");
    assertThat(response.getBody().getMessage())
        .isEqualTo("Account is temporarily locked due to too many failed attempts");
  }

  @Test
  void handleUserNotFound_returns404WithUserNotFoundCode() {
    // Arrange
    UUID userId = UUID.randomUUID();
    UserNotFoundException ex = new UserNotFoundException(userId);

    // Act
    ResponseEntity<Error> response = handler.handleUserNotFound(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("USER_NOT_FOUND");
    assertThat(response.getBody().getMessage()).contains(userId.toString());
  }

  @Test
  void handleValidation_returns400WithFieldError() throws NoSuchMethodException {
    // Arrange
    MethodArgumentNotValidException ex =
        buildValidationException(
            new FieldError("request", "displayName", "size must be between 2 and 50"));

    // Act
    ResponseEntity<Error> response = handler.handleValidation(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed");
    assertThat(response.getBody().getDetails()).hasSize(1);
    assertThat(response.getBody().getDetails().getFirst().getField()).isEqualTo("displayName");
    assertThat(response.getBody().getDetails().getFirst().getMessage())
        .isEqualTo("size must be between 2 and 50");
  }

  @Test
  void handleValidation_returnsMultipleFieldErrors() throws NoSuchMethodException {
    // Arrange
    MethodArgumentNotValidException ex =
        buildValidationException(
            new FieldError("request", "displayName", "size must be between 2 and 50"),
            new FieldError("request", "avatarUrl", "size must be between 0 and 500"));

    // Act
    ResponseEntity<Error> response = handler.handleValidation(ex);

    // Assert
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDetails()).hasSize(2);
    assertThat(response.getBody().getDetails())
        .extracting(com.accountabilityatlas.userservice.web.model.FieldError::getField)
        .containsExactlyInAnyOrder("displayName", "avatarUrl");
  }

  @Test
  void handleValidation_returnsEmptyDetails_whenNoFieldErrors() throws NoSuchMethodException {
    // Arrange
    MethodArgumentNotValidException ex = buildValidationException();

    // Act
    ResponseEntity<Error> response = handler.handleValidation(ex);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getDetails()).isNullOrEmpty();
  }

  private MethodArgumentNotValidException buildValidationException(FieldError... fieldErrors)
      throws NoSuchMethodException {
    BindingResult bindingResult = new MapBindingResult(new java.util.HashMap<>(), "request");
    for (FieldError fieldError : fieldErrors) {
      bindingResult.addError(fieldError);
    }
    MethodParameter param =
        new MethodParameter(
            GlobalExceptionHandlerTest.class.getDeclaredMethod("stubMethod", Object.class), 0);
    return new MethodArgumentNotValidException(param, bindingResult);
  }

  @SuppressWarnings("unused")
  private void stubMethod(Object request) {
    // Stub method to satisfy MethodParameter constructor requirement
  }
}
