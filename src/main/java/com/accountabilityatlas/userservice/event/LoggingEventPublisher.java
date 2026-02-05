package com.accountabilityatlas.userservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEventPublisher implements EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

  @Override
  public void publish(DomainEvent event) {
    log.info("Publishing event: type={}, event={}", event.eventType(), event);
  }
}
