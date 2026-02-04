package com.accountabilityatlas.userservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
  private Duration accessTokenExpiry = Duration.ofMinutes(15);
  private Duration refreshTokenExpiry = Duration.ofDays(7);

  public Duration getAccessTokenExpiry() {
    return accessTokenExpiry;
  }

  public void setAccessTokenExpiry(Duration accessTokenExpiry) {
    this.accessTokenExpiry = accessTokenExpiry;
  }

  public Duration getRefreshTokenExpiry() {
    return refreshTokenExpiry;
  }

  public void setRefreshTokenExpiry(Duration refreshTokenExpiry) {
    this.refreshTokenExpiry = refreshTokenExpiry;
  }
}
