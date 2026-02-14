package com.accountabilityatlas.userservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
    Map<String, Object> result = controller.jwks();

    assertThat(result).containsKey("keys");
    List<Map<String, Object>> keys = (List<Map<String, Object>>) result.get("keys");
    assertThat(keys).hasSize(1);

    Map<String, Object> key = keys.getFirst();
    assertThat(key.get("kty")).isEqualTo("RSA");
    assertThat(key.get("kid")).isEqualTo("user-service-key-1");
    assertThat(key).containsKeys("n", "e");
  }

  @Test
  void jwks_returnsSameResultOnMultipleCalls() {
    Map<String, Object> first = controller.jwks();
    Map<String, Object> second = controller.jwks();

    assertThat(first).isEqualTo(second);
  }
}
