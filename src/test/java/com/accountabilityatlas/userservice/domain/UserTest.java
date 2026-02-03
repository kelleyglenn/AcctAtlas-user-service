package com.accountabilityatlas.userservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void newUser_defaultsTrustTierToNew() {
    User user = new User();
    assertThat(user.getTrustTier()).isEqualTo(TrustTier.NEW);
  }

  @Test
  void newUser_defaultsEmailVerifiedToFalse() {
    User user = new User();
    assertThat(user.isEmailVerified()).isFalse();
  }

  @Test
  void newUser_passwordHashIsNullable() {
    User user = new User();
    assertThat(user.getPasswordHash()).isNull();
  }
}
