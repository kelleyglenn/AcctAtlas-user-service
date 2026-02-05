package com.accountabilityatlas.userservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.exception.EmailAlreadyExistsException;
import com.accountabilityatlas.userservice.exception.GlobalExceptionHandler;
import com.accountabilityatlas.userservice.exception.InvalidCredentialsException;
import com.accountabilityatlas.userservice.service.AuthResult;
import com.accountabilityatlas.userservice.service.AuthenticationService;
import com.accountabilityatlas.userservice.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RegistrationService registrationService;
  @MockitoBean private AuthenticationService authenticationService;

  @Test
  void register_returns201OnSuccess() throws Exception {
    var user = buildDomainUser();
    var result = new AuthResult(user, "access-token", "refresh-token");
    when(registrationService.register(anyString(), anyString(), anyString())).thenReturn(user);
    when(authenticationService.login(anyString(), anyString(), any(), any())).thenReturn(result);

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "test@example.com",
                      "password": "SecurePass123",
                      "displayName": "TestUser"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.email").value("test@example.com"))
        .andExpect(jsonPath("$.tokens.accessToken").exists());
  }

  @Test
  void register_returns409WhenEmailExists() throws Exception {
    when(registrationService.register(anyString(), anyString(), anyString()))
        .thenThrow(new EmailAlreadyExistsException());

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "taken@example.com",
                      "password": "SecurePass123",
                      "displayName": "TestUser"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
  }

  @Test
  void login_returns200OnSuccess() throws Exception {
    var user = buildDomainUser();
    var result = new AuthResult(user, "access-token", "refresh-token");
    when(authenticationService.login(anyString(), anyString(), any(), any())).thenReturn(result);

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "test@example.com",
                      "password": "SecurePass123"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value("test@example.com"))
        .andExpect(jsonPath("$.tokens.accessToken").value("access-token"));
  }

  @Test
  void login_returns401OnInvalidCredentials() throws Exception {
    when(authenticationService.login(anyString(), anyString(), any(), any()))
        .thenThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "test@example.com",
                      "password": "wrong"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  private com.accountabilityatlas.userservice.domain.User buildDomainUser() {
    var user = new com.accountabilityatlas.userservice.domain.User();
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);
    return user;
  }
}
