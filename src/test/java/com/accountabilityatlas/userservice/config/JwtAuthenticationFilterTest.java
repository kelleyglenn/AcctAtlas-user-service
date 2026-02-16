package com.accountabilityatlas.userservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.config.JwtAuthenticationFilter.JwtAuthenticationToken;
import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private TokenService tokenService;
  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(tokenService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_withValidToken_setsAuthentication() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    String email = "test@example.com";
    String token = "valid-token";

    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(userId.toString());
    when(claims.get("email", String.class)).thenReturn(email);
    when(claims.get("trustTier", String.class)).thenReturn("NEW");
    when(claims.get("sessionId", String.class)).thenReturn(sessionId.toString());

    request.addHeader("Authorization", "Bearer " + token);
    when(tokenService.parseAccessToken(token)).thenReturn(claims);

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .isInstanceOf(JwtAuthenticationToken.class);
    JwtAuthenticationToken auth =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth.getUserId()).isEqualTo(userId);
    assertThat(auth.getEmail()).isEqualTo(email);
    assertThat(auth.getTrustTier()).isEqualTo(TrustTier.NEW);
    assertThat(auth.getSessionId()).isEqualTo(sessionId);
  }

  @Test
  void doFilterInternal_withNoAuthHeader_doesNotSetAuthentication() throws Exception {
    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterInternal_withNonBearerAuth_doesNotSetAuthentication() throws Exception {
    request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterInternal_withExpiredToken_returns401() throws Exception {
    String token = "expired-token";
    request.addHeader("Authorization", "Bearer " + token);
    when(tokenService.parseAccessToken(token))
        .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("TOKEN_EXPIRED");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterInternal_withInvalidToken_returns401() throws Exception {
    String token = "invalid-token";
    request.addHeader("Authorization", "Bearer " + token);
    when(tokenService.parseAccessToken(token))
        .thenThrow(new SignatureException("Invalid signature"));

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
