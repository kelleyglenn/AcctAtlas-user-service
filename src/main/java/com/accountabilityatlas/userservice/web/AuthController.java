package com.accountabilityatlas.userservice.web;

import com.accountabilityatlas.userservice.service.AuthResult;
import com.accountabilityatlas.userservice.service.AuthenticationService;
import com.accountabilityatlas.userservice.service.RegistrationService;
import com.accountabilityatlas.userservice.web.api.AuthenticationApi;
import com.accountabilityatlas.userservice.web.model.LoginRequest;
import com.accountabilityatlas.userservice.web.model.LoginResponse;
import com.accountabilityatlas.userservice.web.model.OAuthProvider;
import com.accountabilityatlas.userservice.web.model.OAuthRequest;
import com.accountabilityatlas.userservice.web.model.OAuthResponse;
import com.accountabilityatlas.userservice.web.model.PasswordResetConfirmRequest;
import com.accountabilityatlas.userservice.web.model.PasswordResetRequest;
import com.accountabilityatlas.userservice.web.model.RefreshRequest;
import com.accountabilityatlas.userservice.web.model.RefreshResponse;
import com.accountabilityatlas.userservice.web.model.RegisterRequest;
import com.accountabilityatlas.userservice.web.model.RegisterResponse;
import com.accountabilityatlas.userservice.web.model.RequestPasswordReset202Response;
import com.accountabilityatlas.userservice.web.model.TokenPair;
import com.accountabilityatlas.userservice.web.model.User;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthenticationApi {

  private final RegistrationService registrationService;
  private final AuthenticationService authenticationService;

  public AuthController(
      RegistrationService registrationService, AuthenticationService authenticationService) {
    this.registrationService = registrationService;
    this.authenticationService = authenticationService;
  }

  @Override
  public ResponseEntity<RegisterResponse> registerUser(RegisterRequest request) {
    registrationService.register(
        request.getEmail(), request.getPassword(), request.getDisplayName());

    AuthResult authResult =
        authenticationService.login(request.getEmail(), request.getPassword(), null, null);

    RegisterResponse response = new RegisterResponse();
    response.setUser(toApiUser(authResult.user()));
    response.setTokens(toTokenPair(authResult.accessToken(), authResult.refreshToken()));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<LoginResponse> loginUser(LoginRequest request) {
    AuthResult result =
        authenticationService.login(request.getEmail(), request.getPassword(), null, null);

    LoginResponse response = new LoginResponse();
    response.setUser(toApiUser(result.user()));
    response.setTokens(toTokenPair(result.accessToken(), result.refreshToken()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> logoutUser() {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<OAuthResponse> oauthLogin(
      OAuthProvider provider, OAuthRequest oauthRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<RefreshResponse> refreshTokens(RefreshRequest refreshRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<RequestPasswordReset202Response> requestPasswordReset(
      PasswordResetRequest passwordResetRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<RequestPasswordReset202Response> confirmPasswordReset(
      PasswordResetConfirmRequest passwordResetConfirmRequest) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  private User toApiUser(com.accountabilityatlas.userservice.domain.User domainUser) {
    User apiUser = new User();
    apiUser.setId(domainUser.getId());
    apiUser.setEmail(domainUser.getEmail());
    apiUser.setEmailVerified(domainUser.isEmailVerified());
    apiUser.setDisplayName(domainUser.getDisplayName());
    if (domainUser.getAvatarUrl() != null) {
      apiUser.setAvatarUrl(URI.create(domainUser.getAvatarUrl()));
    }
    apiUser.setTrustTier(
        com.accountabilityatlas.userservice.web.model.TrustTier.fromValue(
            domainUser.getTrustTier().name()));
    if (domainUser.getCreatedAt() != null) {
      apiUser.setCreatedAt(OffsetDateTime.ofInstant(domainUser.getCreatedAt(), ZoneOffset.UTC));
    }
    return apiUser;
  }

  private TokenPair toTokenPair(String accessToken, String refreshToken) {
    TokenPair tokens = new TokenPair();
    tokens.setAccessToken(accessToken);
    tokens.setRefreshToken(refreshToken);
    tokens.setExpiresIn(900);
    tokens.setTokenType("Bearer");
    return tokens;
  }
}
