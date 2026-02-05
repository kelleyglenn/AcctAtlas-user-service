package com.accountabilityatlas.userservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email, Instant timestamp)
    implements DomainEvent {
  @Override
  public String eventType() {
    return "UserRegistered";
  }
}
