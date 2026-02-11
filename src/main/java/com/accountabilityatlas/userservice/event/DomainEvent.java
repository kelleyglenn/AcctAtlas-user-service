package com.accountabilityatlas.userservice.event;

import java.time.Instant;

public sealed interface DomainEvent permits UserRegisteredEvent, UserTrustTierChangedEvent {
  String eventType();

  Instant timestamp();
}
