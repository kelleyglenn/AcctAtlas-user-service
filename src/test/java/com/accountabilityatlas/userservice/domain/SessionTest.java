package com.accountabilityatlas.userservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SessionTest {

  @Test
  void isValid_returnsTrueWhenNotRevokedAndNotExpired() {
    Session session = new Session();
    session.setExpiresAt(Instant.now().plusSeconds(3600));

    assertThat(session.isValid(Instant.now())).isTrue();
  }

  @Test
  void isValid_returnsFalseWhenRevoked() {
    Session session = new Session();
    session.setExpiresAt(Instant.now().plusSeconds(3600));
    session.setRevokedAt(Instant.now());

    assertThat(session.isValid(Instant.now())).isFalse();
  }

  @Test
  void isValid_returnsFalseWhenExpired() {
    Session session = new Session();
    session.setExpiresAt(Instant.now().minusSeconds(1));

    assertThat(session.isValid(Instant.now())).isFalse();
  }
}
