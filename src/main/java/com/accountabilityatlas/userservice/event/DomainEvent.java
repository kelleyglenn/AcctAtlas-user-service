package com.accountabilityatlas.userservice.event;

import java.time.Instant;

public sealed interface DomainEvent permits UserRegisteredEvent {
  String eventType();

  Instant timestamp();
}
