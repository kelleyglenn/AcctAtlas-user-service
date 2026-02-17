package com.accountabilityatlas.userservice.web;

import com.accountabilityatlas.userservice.config.JwtAuthenticationFilter.JwtAuthenticationToken;
import com.accountabilityatlas.userservice.domain.UserPrivacySettings;
import com.accountabilityatlas.userservice.domain.UserSocialLinks;
import com.accountabilityatlas.userservice.domain.Visibility;
import com.accountabilityatlas.userservice.service.AvatarService;
import com.accountabilityatlas.userservice.service.UserService;
import com.accountabilityatlas.userservice.web.api.AdminApi;
import com.accountabilityatlas.userservice.web.api.UsersApi;
import com.accountabilityatlas.userservice.web.model.PrivacySettings;
import com.accountabilityatlas.userservice.web.model.SocialLinks;
import com.accountabilityatlas.userservice.web.model.TrustTier;
import com.accountabilityatlas.userservice.web.model.UpdateTrustTierRequest;
import com.accountabilityatlas.userservice.web.model.UpdateUserRequest;
import com.accountabilityatlas.userservice.web.model.User;
import com.accountabilityatlas.userservice.web.model.UserPublicProfile;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersController implements UsersApi, AdminApi {

  private final UserService userService;
  private final AvatarService avatarService;

  public UsersController(UserService userService, AvatarService avatarService) {
    this.userService = userService;
    this.avatarService = avatarService;
  }

  @Override
  public ResponseEntity<User> getCurrentUser() {
    JwtAuthenticationToken auth =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    UUID userId = auth.getUserId();

    com.accountabilityatlas.userservice.domain.User domainUser = userService.getUserById(userId);
    User apiUser = toApiUser(domainUser);
    enrichUserWithProfileData(apiUser, userId, domainUser.getEmail());
    return ResponseEntity.ok(apiUser);
  }

  @Override
  public ResponseEntity<UserPublicProfile> getUserById(UUID id) {
    com.accountabilityatlas.userservice.domain.User domainUser = userService.getUserById(id);
    UserPublicProfile profile = toPublicProfile(domainUser);

    boolean viewerIsRegistered = getCurrentUserIdOrNull() != null;
    UserPrivacySettings privacy = userService.getPrivacySettings(id);

    boolean showSocialLinks =
        privacy.getSocialLinksVisibility() == Visibility.PUBLIC
            || (privacy.getSocialLinksVisibility() == Visibility.REGISTERED && viewerIsRegistered);

    if (showSocialLinks) {
      userService
          .getSocialLinks(id)
          .ifPresent(links -> profile.setSocialLinks(toApiSocialLinks(links)));
    }

    return ResponseEntity.ok(profile);
  }

  @Override
  public ResponseEntity<User> updateCurrentUser(UpdateUserRequest updateUserRequest) {
    JwtAuthenticationToken auth =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    UUID userId = auth.getUserId();

    com.accountabilityatlas.userservice.domain.User domainUser =
        userService.updateProfile(userId, updateUserRequest);

    User apiUser = toApiUser(domainUser);
    enrichUserWithProfileData(apiUser, userId, domainUser.getEmail());
    return ResponseEntity.ok(apiUser);
  }

  @Override
  public ResponseEntity<User> updateUserTrustTier(UUID id, UpdateTrustTierRequest request) {
    com.accountabilityatlas.userservice.domain.TrustTier newTier =
        com.accountabilityatlas.userservice.domain.TrustTier.valueOf(request.getTrustTier().name());
    com.accountabilityatlas.userservice.domain.User updated =
        userService.updateTrustTier(id, newTier, request.getReason());
    return ResponseEntity.ok(toApiUser(updated));
  }

  private UserPublicProfile toPublicProfile(
      com.accountabilityatlas.userservice.domain.User domainUser) {
    UserPublicProfile profile = new UserPublicProfile();
    profile.setId(domainUser.getId());
    profile.setDisplayName(domainUser.getDisplayName());
    if (domainUser.getAvatarUrl() != null) {
      profile.setAvatarUrl(URI.create(domainUser.getAvatarUrl()));
    }
    if (domainUser.getCreatedAt() != null) {
      OffsetDateTime created = OffsetDateTime.ofInstant(domainUser.getCreatedAt(), ZoneOffset.UTC);
      profile.setCreatedAt(created);
      profile.setMemberSince(created);
    }

    if (domainUser.getStats() != null) {
      profile.setApprovedVideoCount(domainUser.getStats().getApprovedCount());
    }

    return profile;
  }

  private void enrichUserWithProfileData(User apiUser, UUID userId, String email) {
    UserSocialLinks socialLinks = userService.getSocialLinks(userId).orElse(null);
    if (socialLinks != null) {
      apiUser.setSocialLinks(toApiSocialLinks(socialLinks));
    }
    apiUser.setPrivacySettings(toApiPrivacySettings(userService.getPrivacySettings(userId)));
    apiUser.setAvatarSources(avatarService.getAvatarSources(email, socialLinks));
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

  private SocialLinks toApiSocialLinks(UserSocialLinks links) {
    SocialLinks api = new SocialLinks();
    api.setYoutube(links.getYoutube());
    api.setFacebook(links.getFacebook());
    api.setInstagram(links.getInstagram());
    api.setTiktok(links.getTiktok());
    api.setxTwitter(links.getXTwitter());
    api.setBluesky(links.getBluesky());
    return api;
  }

  private PrivacySettings toApiPrivacySettings(UserPrivacySettings settings) {
    PrivacySettings api = new PrivacySettings();
    api.setSocialLinksVisibility(
        PrivacySettings.SocialLinksVisibilityEnum.fromValue(
            settings.getSocialLinksVisibility().name()));
    api.setSubmissionsVisibility(
        PrivacySettings.SubmissionsVisibilityEnum.fromValue(
            settings.getSubmissionsVisibility().name()));
    return api;
  }

  private UUID getCurrentUserIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      return jwt.getUserId();
    }
    return null;
  }
}
