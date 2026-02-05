package com.accountabilityatlas.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountabilityatlas.userservice.config.JwtProperties;
import com.accountabilityatlas.userservice.domain.TrustTier;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

  private TokenService tokenService;
  private KeyPair keyPair;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    keyPair = generator.generateKeyPair();

    JwtProperties properties = new JwtProperties();
    properties.setAccessTokenExpiry(Duration.ofMinutes(15));
    properties.setRefreshTokenExpiry(Duration.ofDays(7));

    tokenService = new TokenService(properties, keyPair);
  }

  @Test
  void generateAccessToken_containsExpectedClaims() {
    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();

    String token =
        tokenService.generateAccessToken(userId, "test@example.com", TrustTier.NEW, sessionId);

    var claims = tokenService.parseAccessToken(token);
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
    assertThat(claims.get("trustTier", String.class)).isEqualTo("NEW");
    assertThat(claims.get("sessionId", String.class)).isEqualTo(sessionId.toString());
  }

  @Test
  void generateRefreshToken_returnsNonEmptyString() {
    String refreshToken = tokenService.generateRefreshToken();
    assertThat(refreshToken).isNotBlank();
    assertThat(refreshToken.length()).isGreaterThan(20);
  }

  @Test
  void hashRefreshToken_returnsDeterministicHash() {
    String token = "test-refresh-token";
    String hash1 = tokenService.hashRefreshToken(token);
    String hash2 = tokenService.hashRefreshToken(token);
    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).isNotEqualTo(token);
  }
}
