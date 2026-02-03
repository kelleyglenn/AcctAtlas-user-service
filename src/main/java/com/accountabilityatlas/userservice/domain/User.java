package com.accountabilityatlas.userservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "users", schema = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "trust_tier", nullable = false, length = 20)
  private TrustTier trustTier = TrustTier.NEW;

  @Column(
      name = "sys_period",
      insertable = false,
      updatable = false,
      columnDefinition = "tstzrange")
  private String sysPeriod;

  @Formula("lower(sys_period)")
  private Instant createdAt;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private UserStats stats;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<OAuthLink> oauthLinks = new ArrayList<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public TrustTier getTrustTier() {
    return trustTier;
  }

  public void setTrustTier(TrustTier trustTier) {
    this.trustTier = trustTier;
  }

  public String getSysPeriod() {
    return sysPeriod;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public UserStats getStats() {
    return stats;
  }

  public void setStats(UserStats stats) {
    this.stats = stats;
  }

  public List<OAuthLink> getOauthLinks() {
    return oauthLinks;
  }

  public void setOauthLinks(List<OAuthLink> oauthLinks) {
    this.oauthLinks = oauthLinks;
  }
}
