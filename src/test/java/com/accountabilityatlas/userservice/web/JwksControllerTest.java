package com.accountabilityatlas.userservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class JwksControllerTest {

  private static JwksController controller;

  @BeforeAll
  static void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    controller = new JwksController(keyPair);
  }

  @Test
  @SuppressWarnings("unchecked")
  void jwks_returnsValidJwkSet() {
    ResponseEntity<Map<String, Object>> response = controller.jwks();
    Map<String, Object> result = response.getBody();

    assertThat(result).containsKey("keys");
    List<Map<String, Object>> keys = (List<Map<String, Object>>) result.get("keys");
    assertThat(keys).hasSize(1);

    Map<String, Object> key = keys.getFirst();
    assertThat(key.get("kty")).isEqualTo("RSA");
    assertThat(key.get("kid")).isEqualTo("user-service-key-1");
    assertThat(key).containsKeys("n", "e");
  }

  @Test
  @SuppressWarnings("unchecked")
  void jwks_doesNotExposePrivateKeyComponents() {
    ResponseEntity<Map<String, Object>> response = controller.jwks();
    Map<String, Object> result = response.getBody();
    List<Map<String, Object>> keys = (List<Map<String, Object>>) result.get("keys");
    Map<String, Object> key = keys.getFirst();

    assertThat(key).doesNotContainKeys("d", "p", "q", "dp", "dq", "qi");
  }

  @Test
  void jwks_returnsSameResultOnMultipleCalls() {
    ResponseEntity<Map<String, Object>> first = controller.jwks();
    ResponseEntity<Map<String, Object>> second = controller.jwks();

    assertThat(first.getBody()).isEqualTo(second.getBody());
  }

  @Test
  void jwks_returnsCacheControlHeader() {
    ResponseEntity<Map<String, Object>> response = controller.jwks();

    assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=3600");
  }
}
