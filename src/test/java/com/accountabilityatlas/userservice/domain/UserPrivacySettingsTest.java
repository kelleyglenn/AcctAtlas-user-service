package com.accountabilityatlas.userservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserPrivacySettingsTest {

  @Test
  void defaults_socialLinksRegistered_submissionsPublic() {
    var settings = new UserPrivacySettings();
    assertThat(settings.getSocialLinksVisibility()).isEqualTo(Visibility.REGISTERED);
    assertThat(settings.getSubmissionsVisibility()).isEqualTo(Visibility.PUBLIC);
  }
}
