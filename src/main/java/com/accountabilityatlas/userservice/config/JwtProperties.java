package com.accountabilityatlas.userservice.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {
  private Duration accessTokenExpiry = Duration.ofMinutes(15);
  private Duration refreshTokenExpiry = Duration.ofDays(7);
}
