package com.accountabilityatlas.userservice.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

  private final Map<String, Object> jwkSet;

  public JwksController(KeyPair keyPair) {
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAKey rsaKey = new RSAKey.Builder(publicKey).keyID("user-service-key-1").build();
    this.jwkSet = new JWKSet(rsaKey).toJSONObject();
  }

  @GetMapping("/.well-known/jwks.json")
  public Map<String, Object> jwks() {
    return jwkSet;
  }
}
