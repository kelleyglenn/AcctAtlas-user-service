package com.accountabilityatlas.userservice.web;

import com.accountabilityatlas.userservice.config.JwtAuthenticationFilter.JwtAuthenticationToken;
import com.accountabilityatlas.userservice.service.UserService;
import com.accountabilityatlas.userservice.web.api.UsersApi;
import com.accountabilityatlas.userservice.web.model.TrustTier;
import com.accountabilityatlas.userservice.web.model.UpdateUserRequest;
import com.accountabilityatlas.userservice.web.model.User;
import com.accountabilityatlas.userservice.web.model.UserPublicProfile;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersController implements UsersApi {

  private final UserService userService;

  public UsersController(UserService userService) {
    this.userService = userService;
  }

  @Override
  public ResponseEntity<User> getCurrentUser() {
    JwtAuthenticationToken auth =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    UUID userId = auth.getUserId();

    com.accountabilityatlas.userservice.domain.User domainUser = userService.getUserById(userId);
    return ResponseEntity.ok(toApiUser(domainUser));
  }

  @Override
  public ResponseEntity<UserPublicProfile> getUserById(UUID id) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<User> updateCurrentUser(UpdateUserRequest updateUserRequest) {
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
    apiUser.setTrustTier(TrustTier.fromValue(domainUser.getTrustTier().name()));
    if (domainUser.getCreatedAt() != null) {
      apiUser.setCreatedAt(OffsetDateTime.ofInstant(domainUser.getCreatedAt(), ZoneOffset.UTC));
    }
    return apiUser;
  }
}
