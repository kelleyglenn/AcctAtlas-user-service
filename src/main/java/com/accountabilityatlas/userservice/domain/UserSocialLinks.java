package com.accountabilityatlas.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_social_links", schema = "users")
@Getter
@Setter
public class UserSocialLinks {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Column(length = 100)
  private String youtube;

  @Column(length = 100)
  private String facebook;

  @Column(length = 50)
  private String instagram;

  @Column(length = 50)
  private String tiktok;

  @Column(name = "x_twitter", length = 50)
  private String xTwitter;

  @Column(length = 100)
  private String bluesky;
}
