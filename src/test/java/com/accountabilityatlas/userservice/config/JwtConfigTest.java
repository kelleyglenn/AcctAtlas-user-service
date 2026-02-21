package com.accountabilityatlas.userservice.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtConfigTest {

  private final JwtConfig jwtConfig = new JwtConfig();

  @Test
  void jwtKeyPair_withNoPrivateKey_generatesNewKeyPair() throws Exception {
    JwtProperties properties = new JwtProperties();

    KeyPair keyPair = jwtConfig.jwtKeyPair(properties);

    assertThat(keyPair).isNotNull();
    assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
    assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
  }

  @Test
  void jwtKeyPair_withConfiguredPrivateKey_loadsThatKey() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair original = generator.generateKeyPair();
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes(UTF_8))
                .encodeToString(original.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";

    JwtProperties properties = new JwtProperties();
    properties.setPrivateKey(pem);

    KeyPair loaded = jwtConfig.jwtKeyPair(properties);

    assertThat(loaded.getPrivate().getEncoded()).isEqualTo(original.getPrivate().getEncoded());
    RSAPublicKey originalPublic = (RSAPublicKey) original.getPublic();
    RSAPublicKey loadedPublic = (RSAPublicKey) loaded.getPublic();
    assertThat(loadedPublic.getModulus()).isEqualTo(originalPublic.getModulus());
    assertThat(loadedPublic.getPublicExponent()).isEqualTo(originalPublic.getPublicExponent());
  }
}
