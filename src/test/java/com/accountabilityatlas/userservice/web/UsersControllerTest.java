package com.accountabilityatlas.userservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accountabilityatlas.userservice.config.JwtAuthenticationFilter;
import com.accountabilityatlas.userservice.config.JwtAuthenticationFilter.JwtAuthenticationToken;
import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.exception.GlobalExceptionHandler;
import com.accountabilityatlas.userservice.exception.UserNotFoundException;
import com.accountabilityatlas.userservice.service.UserService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
}
