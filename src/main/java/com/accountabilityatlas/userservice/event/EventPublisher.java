package com.accountabilityatlas.userservice.event;

public interface EventPublisher {
  void publish(DomainEvent event);
}
