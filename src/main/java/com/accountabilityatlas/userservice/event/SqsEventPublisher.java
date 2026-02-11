package com.accountabilityatlas.userservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * EventPublisher implementation that publishes events to SQS via Spring Cloud Stream.
 *
 * <p>This replaces the LoggingEventPublisher for production use.
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class SqsEventPublisher implements EventPublisher {

  private static final String USER_EVENT_BINDING = "userEvent-out-0";

  private final StreamBridge streamBridge;

  @Override
  public void publish(DomainEvent event) {
    log.info("Publishing {} event to SQS: {}", event.eventType(), event);
    boolean sent = streamBridge.send(USER_EVENT_BINDING, event);
    if (sent) {
      log.debug("Published {} event successfully", event.eventType());
    } else {
      log.error("Failed to publish {} event: {}", event.eventType(), event);
    }
  }
}
