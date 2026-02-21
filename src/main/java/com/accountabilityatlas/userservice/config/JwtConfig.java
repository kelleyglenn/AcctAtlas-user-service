package com.accountabilityatlas.userservice.config;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

  @Bean
  public KeyPair jwtKeyPair(JwtProperties properties) throws Exception {
    if (properties.getPrivateKey() != null && !properties.getPrivateKey().isBlank()) {
      return loadKeyPair(properties.getPrivateKey());
    }
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  private KeyPair loadKeyPair(String pem) throws Exception {
    String base64 =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    byte[] decoded = Base64.getDecoder().decode(base64);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(decoded);
    RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(privateSpec);

    RSAPublicKeySpec publicSpec =
        new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());

    return new KeyPair(keyFactory.generatePublic(publicSpec), privateKey);
  }
}
