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
import com.accountabilityatlas.userservice.domain.UserStats;
import com.accountabilityatlas.userservice.exception.GlobalExceptionHandler;
import com.accountabilityatlas.userservice.exception.UserNotFoundException;
import com.accountabilityatlas.userservice.service.UserService;
import java.time.Instant;
import java.util.Collections;
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

    JwtAuthenticationToken auth =
        new JwtAuthenticationToken(
            userId,
            "test@example.com",
            TrustTier.NEW,
            UUID.randomUUID(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userService.getUserById(userId)).thenReturn(user);

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.displayName").value("TestUser"))
        .andExpect(jsonPath("$.trustTier").value("NEW"));
  }

  @Test
  void getCurrentUser_returns404WhenUserNotFound() throws Exception {
    UUID userId = UUID.randomUUID();

    JwtAuthenticationToken auth =
        new JwtAuthenticationToken(
            userId,
            "test@example.com",
            TrustTier.NEW,
            UUID.randomUUID(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

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

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("PublicUser"))
        .andExpect(jsonPath("$.trustTier").value("TRUSTED"))
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
  void getUserById_mapsAvatarUrlCreatedAtAndStats() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = buildUserWithAllFields(userId);
    UserStats stats = new UserStats();
    stats.setSubmissionCount(10);
    stats.setApprovedCount(8);
    user.setStats(stats);
    when(userService.getUserById(userId)).thenReturn(user);

    mockMvc
        .perform(get("/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.stats.submissionCount").value(10))
        .andExpect(jsonPath("$.stats.approvedCount").value(8));
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
}
