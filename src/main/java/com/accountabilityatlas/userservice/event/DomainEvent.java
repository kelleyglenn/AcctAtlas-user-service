package com.accountabilityatlas.userservice.event;

public sealed interface DomainEvent permits UserRegisteredEvent, UserTrustTierChangedEvent {
  String eventType();
}
