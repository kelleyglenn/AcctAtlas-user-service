package com.accountabilityatlas.userservice.config;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenService tokenService;

  public JwtAuthenticationFilter(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length());

    try {
      Claims claims = tokenService.parseAccessToken(token);

      UUID userId = UUID.fromString(claims.getSubject());
      String email = claims.get("email", String.class);
      TrustTier trustTier = TrustTier.valueOf(claims.get("trustTier", String.class));
      UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));

      JwtAuthenticationToken authentication =
          new JwtAuthenticationToken(
              userId,
              email,
              trustTier,
              sessionId,
              Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (ExpiredJwtException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"code\":\"TOKEN_EXPIRED\",\"message\":\"Access token expired\"}");
      return;
    } catch (JwtException | IllegalArgumentException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"code\":\"INVALID_TOKEN\",\"message\":\"Invalid access token\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  @Getter
  public static class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final UUID userId;
    private final String email;
    private final TrustTier trustTier;
    private final UUID sessionId;

    public JwtAuthenticationToken(
        UUID userId,
        String email,
        TrustTier trustTier,
        UUID sessionId,
        java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
            authorities) {
      super(userId, null, authorities);
      this.userId = userId;
      this.email = email;
      this.trustTier = trustTier;
      this.sessionId = sessionId;
    }
  }
}
