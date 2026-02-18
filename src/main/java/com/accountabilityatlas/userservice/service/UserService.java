package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.domain.UserPrivacySettings;
import com.accountabilityatlas.userservice.domain.UserSocialLinks;
import com.accountabilityatlas.userservice.domain.Visibility;
import com.accountabilityatlas.userservice.event.EventPublisher;
import com.accountabilityatlas.userservice.event.UserTrustTierChangedEvent;
import com.accountabilityatlas.userservice.exception.UserNotFoundException;
import com.accountabilityatlas.userservice.repository.UserPrivacySettingsRepository;
import com.accountabilityatlas.userservice.repository.UserRepository;
import com.accountabilityatlas.userservice.repository.UserSocialLinksRepository;
import com.accountabilityatlas.userservice.web.model.PrivacySettings;
import com.accountabilityatlas.userservice.web.model.SocialLinks;
import com.accountabilityatlas.userservice.web.model.UpdateUserRequest;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UserSocialLinksRepository socialLinksRepository;
  private final UserPrivacySettingsRepository privacySettingsRepository;
  private final EventPublisher eventPublisher;

  public record PublicProfileData(
      User user, UserPrivacySettings privacySettings, Optional<UserSocialLinks> socialLinks) {}

  @Transactional(readOnly = true)
  public User getUserById(UUID id) {
    return getUserByIdInternal(id);
  }

  @Transactional(readOnly = true)
  public PublicProfileData getPublicProfileData(UUID id) {
    User user = getUserByIdInternal(id);
    UserPrivacySettings privacy = getPrivacySettings(id);
    Optional<UserSocialLinks> socialLinks = getSocialLinks(id);
    return new PublicProfileData(user, privacy, socialLinks);
  }

  @Transactional
  public User updateTrustTier(UUID id, TrustTier newTier, String reason) {
    User user = getUserByIdInternal(id);
    TrustTier oldTier = user.getTrustTier();

    if (oldTier == newTier) {
      log.debug("User {} already has trust tier {}, no change needed", id, newTier);
      return user;
    }

    user.setTrustTier(newTier);
    User saved = userRepository.save(user);

    log.info("Updated user {} trust tier: {} -> {} (reason: {})", id, oldTier, newTier, reason);

    // Publish event to notify other services
    UserTrustTierChangedEvent.ChangeReason changeReason = mapReason(reason);
    eventPublisher.publish(
        new UserTrustTierChangedEvent(id, oldTier, newTier, changeReason, Instant.now()));

    return saved;
  }

  @Transactional
  public User updateProfile(UUID userId, UpdateUserRequest request) {
    User user = getUserByIdInternal(userId);

    if (request.getDisplayName() != null) {
      user.setDisplayName(request.getDisplayName());
    }
    if (request.getAvatarUrl() != null) {
      user.setAvatarUrl(emptyToNull(request.getAvatarUrl()));
    }

    userRepository.save(user);

    // Update social links if provided
    if (request.getSocialLinks() != null) {
      UserSocialLinks socialLinks =
          socialLinksRepository
              .findById(userId)
              .orElseGet(
                  () -> {
                    UserSocialLinks newLinks = new UserSocialLinks();
                    newLinks.setUserId(userId);
                    return newLinks;
                  });
      SocialLinks reqLinks = request.getSocialLinks();
      socialLinks.setYoutube(emptyToNull(reqLinks.getYoutube()));
      socialLinks.setFacebook(emptyToNull(reqLinks.getFacebook()));
      socialLinks.setInstagram(emptyToNull(reqLinks.getInstagram()));
      socialLinks.setTiktok(emptyToNull(reqLinks.getTiktok()));
      socialLinks.setXTwitter(emptyToNull(reqLinks.getxTwitter()));
      socialLinks.setBluesky(emptyToNull(reqLinks.getBluesky()));
      socialLinksRepository.save(socialLinks);
    }

    // Update privacy settings if provided
    if (request.getPrivacySettings() != null) {
      UserPrivacySettings privacySettings =
          privacySettingsRepository
              .findById(userId)
              .orElseGet(
                  () -> {
                    UserPrivacySettings newSettings = new UserPrivacySettings();
                    newSettings.setUserId(userId);
                    return newSettings;
                  });
      PrivacySettings reqPrivacy = request.getPrivacySettings();
      if (reqPrivacy.getSocialLinksVisibility() != null) {
        privacySettings.setSocialLinksVisibility(
            Visibility.valueOf(reqPrivacy.getSocialLinksVisibility().name()));
      }
      if (reqPrivacy.getSubmissionsVisibility() != null) {
        privacySettings.setSubmissionsVisibility(
            Visibility.valueOf(reqPrivacy.getSubmissionsVisibility().name()));
      }
      privacySettingsRepository.save(privacySettings);
    }

    return user;
  }

  @Transactional(readOnly = true)
  public Optional<UserSocialLinks> getSocialLinks(UUID userId) {
    return socialLinksRepository.findById(userId);
  }

  @Transactional(readOnly = true)
  public UserPrivacySettings getPrivacySettings(UUID userId) {
    return privacySettingsRepository
        .findById(userId)
        .orElseGet(
            () -> {
              UserPrivacySettings defaults = new UserPrivacySettings();
              defaults.setUserId(userId);
              return defaults;
            });
  }

  private UserTrustTierChangedEvent.ChangeReason mapReason(String reason) {
    if (reason == null) {
      return UserTrustTierChangedEvent.ChangeReason.MANUAL;
    }
    return switch (reason.toUpperCase(Locale.ROOT)) {
      case "AUTO_PROMOTION" -> UserTrustTierChangedEvent.ChangeReason.AUTO_PROMOTION;
      case "AUTO_DEMOTION" -> UserTrustTierChangedEvent.ChangeReason.AUTO_DEMOTION;
      default -> UserTrustTierChangedEvent.ChangeReason.MANUAL;
    };
  }

  private static String emptyToNull(String value) {
    return (value != null && !value.isBlank()) ? value : null;
  }

  private User getUserByIdInternal(UUID id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}
