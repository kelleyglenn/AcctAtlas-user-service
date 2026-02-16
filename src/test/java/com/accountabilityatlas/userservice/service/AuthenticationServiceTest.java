package com.accountabilityatlas.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.config.JwtProperties;
import com.accountabilityatlas.userservice.domain.Session;
import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.exception.InvalidCredentialsException;
import com.accountabilityatlas.userservice.repository.SessionRepository;
import com.accountabilityatlas.userservice.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private SessionRepository sessionRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private TokenService tokenService;
  @Mock private JwtProperties jwtProperties;

  @InjectMocks private AuthenticationService authenticationService;

  @Test
  void login_returnsTokensOnValidCredentials() {
    User user = buildUser("test@example.com", "$2a$12$hashed");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
    when(jwtProperties.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(7));
    when(tokenService.generateAccessToken(any(), anyString(), any(), any()))
        .thenReturn("access-token");
    when(tokenService.generateRefreshToken()).thenReturn("refresh-token");
    when(tokenService.hashRefreshToken("refresh-token")).thenReturn("hashed-refresh");
    when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

    AuthResult result = authenticationService.login("test@example.com", "password123", null, null);

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.user()).isEqualTo(user);
  }

  @Test
  void login_createsSession() {
    User user = buildUser("test@example.com", "$2a$12$hashed");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(true);
    when(jwtProperties.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(7));
    when(tokenService.generateAccessToken(any(), anyString(), any(), any()))
        .thenReturn("access-token");
    when(tokenService.generateRefreshToken()).thenReturn("refresh-token");
    when(tokenService.hashRefreshToken(any())).thenReturn("hashed-refresh");
    when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

    authenticationService.login("test@example.com", "password123", "Chrome", "127.0.0.1");

    verify(sessionRepository).save(any(Session.class));
  }

  @Test
  void login_throwsOnNonExistentEmail() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authenticationService.login("nobody@example.com", "password", null, null))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Email or password is incorrect");
  }

  @Test
  void login_throwsOnWrongPassword() {
    User user = buildUser("test@example.com", "$2a$12$hashed");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "$2a$12$hashed")).thenReturn(false);

    assertThatThrownBy(
            () -> authenticationService.login("test@example.com", "wrong-password", null, null))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Email or password is incorrect");
  }

  @Test
  void login_normalizesEmailToLowercase() {
    User user = buildUser("test@example.com", "$2a$12$hashed");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any())).thenReturn(true);
    when(jwtProperties.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(7));
    when(tokenService.generateAccessToken(any(), anyString(), any(), any()))
        .thenReturn("access-token");
    when(tokenService.generateRefreshToken()).thenReturn("refresh-token");
    when(tokenService.hashRefreshToken(any())).thenReturn("hashed");
    when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    authenticationService.login("Test@Example.COM", "password123", null, null);

    verify(userRepository).findByEmail("test@example.com");
  }

  @Test
  void logout_revokesSessionById() {
    // Arrange
    UUID sessionId = UUID.randomUUID();
    when(sessionRepository.revokeById(eq(sessionId), any(Instant.class))).thenReturn(1);

    // Act
    authenticationService.logout(sessionId);

    // Assert
    verify(sessionRepository).revokeById(eq(sessionId), any(Instant.class));
  }

  @Test
  void logout_handlesAlreadyRevokedSession() {
    // Arrange
    UUID sessionId = UUID.randomUUID();
    when(sessionRepository.revokeById(eq(sessionId), any(Instant.class))).thenReturn(0);

    // Act - should not throw even if session was already revoked
    authenticationService.logout(sessionId);

    // Assert
    verify(sessionRepository).revokeById(eq(sessionId), any(Instant.class));
  }

  private User buildUser(String email, String passwordHash) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordHash);
    user.setDisplayName("Test");
    user.setTrustTier(TrustTier.NEW);
    return user;
  }
}
