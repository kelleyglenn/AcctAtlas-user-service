package com.accountabilityatlas.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserSocialLinksRepository socialLinksRepository;
  @Mock private UserPrivacySettingsRepository privacySettingsRepository;
  @Mock private EventPublisher eventPublisher;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(
            userRepository, socialLinksRepository, privacySettingsRepository, eventPublisher);
  }

  @Test
  void getUserById_returnsUser_whenFound() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    User result = userService.getUserById(userId);

    assertThat(result).isEqualTo(user);
  }

  @Test
  void getUserById_throwsException_whenNotFound() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(userId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(userId.toString());
  }

  @Test
  void updateTrustTier_validTier_updatesTierAndPublishesEvent() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = userService.updateTrustTier(userId, TrustTier.TRUSTED, "AUTO_PROMOTION");

    assertThat(result.getTrustTier()).isEqualTo(TrustTier.TRUSTED);
    verify(userRepository).save(user);

    // Verify event was published
    ArgumentCaptor<UserTrustTierChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(UserTrustTierChangedEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());

    UserTrustTierChangedEvent event = eventCaptor.getValue();
    assertThat(event.userId()).isEqualTo(userId);
    assertThat(event.oldTier()).isEqualTo(TrustTier.NEW);
    assertThat(event.newTier()).isEqualTo(TrustTier.TRUSTED);
    assertThat(event.reason()).isEqualTo(UserTrustTierChangedEvent.ChangeReason.AUTO_PROMOTION);
  }

  @Test
  void updateTrustTier_sameTier_doesNotPublishEvent() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.TRUSTED);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    User result = userService.updateTrustTier(userId, TrustTier.TRUSTED, "MANUAL");

    assertThat(result.getTrustTier()).isEqualTo(TrustTier.TRUSTED);
    verify(userRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void updateTrustTier_userNotFound_throwsException() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateTrustTier(userId, TrustTier.TRUSTED, "reason"))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void updateProfile_updatesDisplayName() {
    UUID userId = UUID.randomUUID();
    User user = buildUser(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setDisplayName("NewName");

    User result = userService.updateProfile(userId, request);

    assertThat(result.getDisplayName()).isEqualTo("NewName");
    verify(userRepository).save(user);
  }

  @Test
  void updateProfile_updatesAvatarUrl() {
    UUID userId = UUID.randomUUID();
    User user = buildUser(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest();
    request.setAvatarUrl(URI.create("https://example.com/new-avatar.png"));

    User result = userService.updateProfile(userId, request);

    assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.png");
  }

  @Test
  void updateProfile_updatesSocialLinks() {
    UUID userId = UUID.randomUUID();
    User user = buildUser(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(socialLinksRepository.findById(userId)).thenReturn(Optional.empty());

    SocialLinks socialLinks = new SocialLinks();
    socialLinks.setYoutube("UCtest123");
    socialLinks.setInstagram("testaccount");

    UpdateUserRequest request = new UpdateUserRequest();
    request.setSocialLinks(socialLinks);

    userService.updateProfile(userId, request);

    ArgumentCaptor<UserSocialLinks> captor = ArgumentCaptor.forClass(UserSocialLinks.class);
    verify(socialLinksRepository).save(captor.capture());

    UserSocialLinks saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getYoutube()).isEqualTo("UCtest123");
    assertThat(saved.getInstagram()).isEqualTo("testaccount");
  }

  @Test
  void updateProfile_updatesPrivacySettings() {
    UUID userId = UUID.randomUUID();
    User user = buildUser(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(privacySettingsRepository.findById(userId)).thenReturn(Optional.empty());

    PrivacySettings privacy = new PrivacySettings();
    privacy.setSocialLinksVisibility(PrivacySettings.SocialLinksVisibilityEnum.PUBLIC);

    UpdateUserRequest request = new UpdateUserRequest();
    request.setPrivacySettings(privacy);

    userService.updateProfile(userId, request);

    ArgumentCaptor<UserPrivacySettings> captor = ArgumentCaptor.forClass(UserPrivacySettings.class);
    verify(privacySettingsRepository).save(captor.capture());

    UserPrivacySettings saved = captor.getValue();
    assertThat(saved.getSocialLinksVisibility()).isEqualTo(Visibility.PUBLIC);
    // submissionsVisibility was not set in the request, so it should keep its default
    assertThat(saved.getSubmissionsVisibility()).isEqualTo(Visibility.PUBLIC);
  }

  @Test
  void updateProfile_userNotFound_throwsException() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    UpdateUserRequest request = new UpdateUserRequest();
    request.setDisplayName("NewName");

    assertThatThrownBy(() -> userService.updateProfile(userId, request))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void getSocialLinks_returnsSocialLinks_whenPresent() {
    UUID userId = UUID.randomUUID();
    UserSocialLinks links = new UserSocialLinks();
    links.setUserId(userId);
    links.setYoutube("UCtest");
    when(socialLinksRepository.findById(userId)).thenReturn(Optional.of(links));

    Optional<UserSocialLinks> result = userService.getSocialLinks(userId);

    assertThat(result).isPresent();
    assertThat(result.get().getYoutube()).isEqualTo("UCtest");
  }

  @Test
  void getSocialLinks_returnsEmpty_whenNotPresent() {
    UUID userId = UUID.randomUUID();
    when(socialLinksRepository.findById(userId)).thenReturn(Optional.empty());

    Optional<UserSocialLinks> result = userService.getSocialLinks(userId);

    assertThat(result).isEmpty();
  }

  @Test
  void getPrivacySettings_returnsSettings_whenPresent() {
    UUID userId = UUID.randomUUID();
    UserPrivacySettings settings = new UserPrivacySettings();
    settings.setUserId(userId);
    settings.setSocialLinksVisibility(Visibility.PUBLIC);
    when(privacySettingsRepository.findById(userId)).thenReturn(Optional.of(settings));

    UserPrivacySettings result = userService.getPrivacySettings(userId);

    assertThat(result.getSocialLinksVisibility()).isEqualTo(Visibility.PUBLIC);
  }

  @Test
  void getPrivacySettings_returnsDefaults_whenNotPresent() {
    UUID userId = UUID.randomUUID();
    when(privacySettingsRepository.findById(userId)).thenReturn(Optional.empty());

    UserPrivacySettings result = userService.getPrivacySettings(userId);

    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getSocialLinksVisibility()).isEqualTo(Visibility.REGISTERED);
    assertThat(result.getSubmissionsVisibility()).isEqualTo(Visibility.PUBLIC);
  }

  private User buildUser(UUID userId) {
    User user = new User();
    user.setId(userId);
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);
    return user;
  }
}
