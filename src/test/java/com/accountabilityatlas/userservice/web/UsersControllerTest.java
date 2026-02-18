package com.accountabilityatlas.userservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accountabilityatlas.userservice.config.JwtAuthenticationFilter;
import com.accountabilityatlas.userservice.config.JwtAuthenticationFilter.JwtAuthenticationToken;
import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.domain.UserPrivacySettings;
import com.accountabilityatlas.userservice.domain.UserSocialLinks;
import com.accountabilityatlas.userservice.domain.UserStats;
import com.accountabilityatlas.userservice.domain.Visibility;
import com.accountabilityatlas.userservice.exception.GlobalExceptionHandler;
import com.accountabilityatlas.userservice.exception.UserNotFoundException;
import com.accountabilityatlas.userservice.service.AvatarService;
import com.accountabilityatlas.userservice.service.UserService;
import com.accountabilityatlas.userservice.web.model.AvatarSources;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsersController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UsersControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @MockitoBean private AvatarService avatarService;

  @SuppressWarnings("UnusedVariable")
  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Test
  void getCurrentUser_returns200WithUserData() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);

    setAuthenticationContext(userId);
    when(userService.getUserById(userId)).thenReturn(user);
    stubDefaultProfileData(userId);

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.displayName").value("TestUser"))
        .andExpect(jsonPath("$.trustTier").value("NEW"))
        .andExpect(jsonPath("$.privacySettings.socialLinksVisibility").value("REGISTERED"))
        .andExpect(jsonPath("$.privacySettings.submissionsVisibility").value("PUBLIC"));
  }

  @Test
  void getCurrentUser_returns404WhenUserNotFound() throws Exception {
    UUID userId = UUID.randomUUID();
    setAuthenticationContext(userId);

    when(userService.getUserById(any())).thenThrow(new UserNotFoundException(userId));

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void getCurrentUser_mapsAvatarUrlAndCreatedAt() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    setAuthenticationContext(userId);
    when(userService.getUserById(userId)).thenReturn(user);
    stubDefaultProfileData(userId);

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void getUserById_returns200WithPublicProfile() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setDisplayName("PublicUser");
    user.setTrustTier(TrustTier.TRUSTED);
    ReflectionTestUtils.setField(user, "id", userId);
    when(userService.getUserById(userId)).thenReturn(user);
    when(userService.getPrivacySettings(userId)).thenReturn(buildDefaultPrivacySettings(userId));

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("PublicUser"))
        .andExpect(jsonPath("$.email").doesNotExist());
  }

  @Test
  void getUserById_returns404WhenUserNotFound() throws Exception {
    UUID userId = UUID.randomUUID();
    when(userService.getUserById(userId)).thenThrow(new UserNotFoundException(userId));

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void getUserById_mapsAvatarUrlCreatedAtAndApprovedVideoCount() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    UserStats stats = new UserStats();
    stats.setSubmissionCount(10);
    stats.setApprovedCount(8);
    user.setStats(stats);
    when(userService.getUserById(userId)).thenReturn(user);
    when(userService.getPrivacySettings(userId)).thenReturn(buildDefaultPrivacySettings(userId));

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.memberSince").exists())
        .andExpect(jsonPath("$.approvedVideoCount").value(8));
  }

  @Test
  void getUserById_showsSocialLinks_whenVisibilityIsPublic() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    when(userService.getUserById(userId)).thenReturn(user);

    UserPrivacySettings privacy = buildDefaultPrivacySettings(userId);
    privacy.setSocialLinksVisibility(Visibility.PUBLIC);
    when(userService.getPrivacySettings(userId)).thenReturn(privacy);

    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(userId);
    socialLinks.setYoutube("UCtest");
    when(userService.getSocialLinks(userId)).thenReturn(Optional.of(socialLinks));

    // No auth set - anonymous viewer
    SecurityContextHolder.clearContext();

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.socialLinks.youtube").value("UCtest"));
  }

  @Test
  void getUserById_hidesSocialLinks_whenVisibilityIsRegistered_andViewerAnonymous()
      throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    when(userService.getUserById(userId)).thenReturn(user);

    // Social links visibility = REGISTERED (default)
    when(userService.getPrivacySettings(userId)).thenReturn(buildDefaultPrivacySettings(userId));

    // No auth set - anonymous viewer
    SecurityContextHolder.clearContext();

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.socialLinks").doesNotExist());
  }

  @Test
  void getUserById_hidesTrustTier_whenViewerAnonymous() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    user.setTrustTier(TrustTier.TRUSTED);
    when(userService.getUserById(userId)).thenReturn(user);
    when(userService.getPrivacySettings(userId)).thenReturn(buildDefaultPrivacySettings(userId));

    SecurityContextHolder.clearContext();

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trustTier").doesNotExist());
  }

  @Test
  void getUserById_showsTrustTier_whenViewerAuthenticated() throws Exception {
    UUID profileUserId = UUID.randomUUID();
    UUID viewerUserId = UUID.randomUUID();
    User user = buildUserWithAllFields(profileUserId);
    user.setTrustTier(TrustTier.TRUSTED);
    when(userService.getUserById(profileUserId)).thenReturn(user);
    when(userService.getPrivacySettings(profileUserId))
        .thenReturn(buildDefaultPrivacySettings(profileUserId));

    setAuthenticationContext(viewerUserId);

    mockMvc
        .perform(get("/users/{id}", profileUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trustTier").value("TRUSTED"));
  }

  @Test
  void getUserById_showsSocialLinks_whenVisibilityIsRegistered_andViewerAuthenticated()
      throws Exception {
    UUID profileUserId = UUID.randomUUID();
    UUID viewerUserId = UUID.randomUUID();
    User user = buildUserWithAllFields(profileUserId);
    when(userService.getUserById(profileUserId)).thenReturn(user);

    // Social links visibility = REGISTERED (default)
    when(userService.getPrivacySettings(profileUserId))
        .thenReturn(buildDefaultPrivacySettings(profileUserId));

    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(profileUserId);
    socialLinks.setInstagram("testaccount");
    when(userService.getSocialLinks(profileUserId)).thenReturn(Optional.of(socialLinks));

    // Set viewer as authenticated
    setAuthenticationContext(viewerUserId);

    mockMvc
        .perform(get("/users/{id}", profileUserId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.socialLinks.instagram").value("testaccount"));
  }

  @Test
  void updateUserTrustTier_returns200() throws Exception {
    UUID userId = UUID.randomUUID();
    User updated = new User();
    updated.setEmail("user@example.com");
    updated.setDisplayName("PromotedUser");
    updated.setTrustTier(TrustTier.TRUSTED);
    when(userService.updateTrustTier(eq(userId), eq(TrustTier.TRUSTED), any())).thenReturn(updated);

    mockMvc
        .perform(
            put("/users/{id}/trust-tier", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"trustTier": "TRUSTED", "reason": "Good contributor"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("PromotedUser"))
        .andExpect(jsonPath("$.trustTier").value("TRUSTED"));

    verify(userService).updateTrustTier(userId, TrustTier.TRUSTED, "Good contributor");
  }

  @Test
  void updateCurrentUser_returns200WithUpdatedProfile() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    user.setDisplayName("UpdatedName");
    setAuthenticationContext(userId);

    when(userService.updateProfile(eq(userId), any())).thenReturn(user);
    stubDefaultProfileData(userId);

    mockMvc
        .perform(
            put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName": "UpdatedName"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("UpdatedName"))
        .andExpect(jsonPath("$.privacySettings.socialLinksVisibility").value("REGISTERED"));

    verify(userService).updateProfile(eq(userId), any());
  }

  @Test
  void updateCurrentUser_withSocialLinks_returns200() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    setAuthenticationContext(userId);

    when(userService.updateProfile(eq(userId), any())).thenReturn(user);

    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(userId);
    socialLinks.setYoutube("UCtest123");
    when(userService.getSocialLinks(userId)).thenReturn(Optional.of(socialLinks));
    when(userService.getPrivacySettings(userId)).thenReturn(buildDefaultPrivacySettings(userId));
    when(avatarService.getAvatarSources(any(), any())).thenReturn(new AvatarSources());

    mockMvc
        .perform(
            put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"socialLinks": {"youtube": "UCtest123"}}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.socialLinks.youtube").value("UCtest123"));
  }

  @Test
  void getCurrentUser_includesSocialLinksAndPrivacy() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    setAuthenticationContext(userId);

    when(userService.getUserById(userId)).thenReturn(user);

    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(userId);
    socialLinks.setInstagram("testaccount");
    socialLinks.setXTwitter("testhandle");
    when(userService.getSocialLinks(userId)).thenReturn(Optional.of(socialLinks));
    when(userService.getPrivacySettings(userId)).thenReturn(buildDefaultPrivacySettings(userId));
    when(avatarService.getAvatarSources(any(), any())).thenReturn(new AvatarSources());

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.socialLinks.instagram").value("testaccount"))
        .andExpect(jsonPath("$.socialLinks.xTwitter").value("testhandle"))
        .andExpect(jsonPath("$.privacySettings.socialLinksVisibility").value("REGISTERED"));
  }

  private void setAuthenticationContext(UUID userId) {
    JwtAuthenticationToken auth =
        new JwtAuthenticationToken(
            userId,
            "test@example.com",
            TrustTier.NEW,
            UUID.randomUUID(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private User buildUserWithAllFields(UUID userId) {
    User user = new User();
    ReflectionTestUtils.setField(user, "id", userId);
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);
    user.setAvatarUrl("https://example.com/avatar.png");
    ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-01-15T10:00:00Z"));
    return user;
  }

  private void stubDefaultProfileData(UUID userId) {
    when(userService.getSocialLinks(userId)).thenReturn(Optional.empty());
    when(userService.getPrivacySettings(userId)).thenReturn(buildDefaultPrivacySettings(userId));
    when(avatarService.getAvatarSources(any(), any())).thenReturn(new AvatarSources());
  }

  private UserPrivacySettings buildDefaultPrivacySettings(UUID userId) {
    UserPrivacySettings settings = new UserPrivacySettings();
    settings.setUserId(userId);
    return settings;
  }
}
