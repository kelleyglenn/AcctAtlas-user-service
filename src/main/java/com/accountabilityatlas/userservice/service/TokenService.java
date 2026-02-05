package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.config.JwtProperties;
import com.accountabilityatlas.userservice.domain.TrustTier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private final JwtProperties properties;
  private final KeyPair keyPair;
  private final SecureRandom secureRandom = new SecureRandom();

  public TokenService(JwtProperties properties, KeyPair keyPair) {
    this.properties = properties;
    this.keyPair = keyPair;
  }

  public String generateAccessToken(
      UUID userId, String email, TrustTier trustTier, UUID sessionId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("trustTier", trustTier.name())
        .claim("sessionId", sessionId.toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(properties.getAccessTokenExpiry())))
        .signWith(keyPair.getPrivate())
        .compact();
  }

  public Claims parseAccessToken(String token) {
    return Jwts.parser()
        .verifyWith(keyPair.getPublic())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public String generateRefreshToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public String hashRefreshToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
