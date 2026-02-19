package com.accountabilityatlas.userservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserSocialLinksTest {

  @Test
  void defaults_allFieldsNull() {
    var links = new UserSocialLinks();
    assertThat(links.getYoutube()).isNull();
    assertThat(links.getFacebook()).isNull();
    assertThat(links.getInstagram()).isNull();
    assertThat(links.getTiktok()).isNull();
    assertThat(links.getXTwitter()).isNull();
    assertThat(links.getBluesky()).isNull();
  }
}
