package com.accountabilityatlas.userservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accountabilityatlas.userservice.event.EventPublisher;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("user_service")
          .withUsername("user_service")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);
    // Exclude Redis auto-configuration since no Redis container is provided
    registry.add(
        "spring.autoconfigure.exclude",
        () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EventPublisher eventPublisher;

  @Test
  void registerThenLogin_fullFlow() throws Exception {
    // Register
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "integration@example.com",
                      "password": "SecurePass123",
                      "displayName": "IntegrationUser"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.email").value("integration@example.com"))
        .andExpect(jsonPath("$.user.trustTier").value("NEW"));

    // Login with same credentials
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "integration@example.com",
                      "password": "SecurePass123"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tokens.accessToken").exists())
        .andExpect(jsonPath("$.tokens.refreshToken").exists());
  }

  @Test
  void register_duplicateEmailReturns409() throws Exception {
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "duplicate@example.com",
                      "password": "SecurePass123",
                      "displayName": "FirstUser"
                    }
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "duplicate@example.com",
                      "password": "SecurePass456",
                      "displayName": "SecondUser"
                    }
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void login_wrongPasswordReturns401() throws Exception {
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "wrongpw@example.com",
                      "password": "SecurePass123",
                      "displayName": "TestUser"
                    }
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "wrongpw@example.com",
                      "password": "WrongPassword"
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerLoginAndGetMe_fullFlow() throws Exception {
    // Register
    MvcResult registerResult =
        mockMvc
            .perform(
                post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "fullflow@example.com",
                          "password": "SecurePass123",
                          "displayName": "FullFlowUser"
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn();

    String accessToken =
        JsonPath.read(registerResult.getResponse().getContentAsString(), "$.tokens.accessToken");

    // Get current user with token
    mockMvc
        .perform(get("/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("fullflow@example.com"))
        .andExpect(jsonPath("$.displayName").value("FullFlowUser"))
        .andExpect(jsonPath("$.trustTier").value("NEW"));
  }

  @Test
  void getUsersMe_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void getUsersMe_withInvalidToken_returns401() throws Exception {
    mockMvc
        .perform(get("/users/me").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void jwksEndpoint_returnsValidJwkSet() throws Exception {
    mockMvc
        .perform(get("/.well-known/jwks.json"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.keys").isArray())
        .andExpect(jsonPath("$.keys.length()").value(1))
        .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
        .andExpect(jsonPath("$.keys[0].kid").value("user-service-key-1"))
        .andExpect(jsonPath("$.keys[0].n").exists())
        .andExpect(jsonPath("$.keys[0].e").exists());
  }

  @Test
  void jwksEndpoint_isPublicWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/.well-known/jwks.json")).andExpect(status().isOk());
  }

  @Test
  void jwksEndpoint_returnsConsistentKeys() throws Exception {
    MvcResult first =
        mockMvc.perform(get("/.well-known/jwks.json")).andExpect(status().isOk()).andReturn();

    MvcResult second =
        mockMvc.perform(get("/.well-known/jwks.json")).andExpect(status().isOk()).andReturn();

    String firstBody = first.getResponse().getContentAsString();
    String secondBody = second.getResponse().getContentAsString();

    assertThat(firstBody).isEqualTo(secondBody);
  }
}
