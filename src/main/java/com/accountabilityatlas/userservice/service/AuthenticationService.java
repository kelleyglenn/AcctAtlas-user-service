package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.config.JwtProperties;
import com.accountabilityatlas.userservice.domain.Session;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.exception.InvalidCredentialsException;
import com.accountabilityatlas.userservice.repository.SessionRepository;
import com.accountabilityatlas.userservice.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final JwtProperties jwtProperties;

  public AuthenticationService(
      UserRepository userRepository,
      SessionRepository sessionRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      JwtProperties jwtProperties) {
    this.userRepository = userRepository;
    this.sessionRepository = sessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.jwtProperties = jwtProperties;
  }

  @Transactional
  public AuthResult login(String email, String password, String deviceInfo, String ipAddress) {
    String normalizedEmail = email.toLowerCase(Locale.ROOT);

    User user =
        userRepository.findByEmail(normalizedEmail).orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    String refreshToken = tokenService.generateRefreshToken();
    String refreshTokenHash = tokenService.hashRefreshToken(refreshToken);

    Session session = new Session();
    session.setUserId(user.getId());
    session.setRefreshTokenHash(refreshTokenHash);
    session.setDeviceInfo(deviceInfo);
    session.setIpAddress(ipAddress);
    session.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenExpiry()));
    session = sessionRepository.save(session);

    String accessToken =
        tokenService.generateAccessToken(
            user.getId(), user.getEmail(), user.getTrustTier(), session.getId());

    return new AuthResult(user, accessToken, refreshToken);
  }
}
