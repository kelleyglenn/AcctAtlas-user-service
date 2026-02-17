package com.accountabilityatlas.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_privacy_settings", schema = "users")
@Getter
@Setter
public class UserPrivacySettings {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "social_links_visibility", nullable = false)
  private Visibility socialLinksVisibility = Visibility.REGISTERED;

  @Enumerated(EnumType.STRING)
  @Column(name = "submissions_visibility", nullable = false)
  private Visibility submissionsVisibility = Visibility.PUBLIC;
}
