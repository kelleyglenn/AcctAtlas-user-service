package com.accountabilityatlas.userservice.event;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * EventPublisher implementation that publishes events to SQS via AWS Spring SqsTemplate.
 *
 * <p>This replaces the LoggingEventPublisher for production use.
 */
@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class SqsEventPublisher implements EventPublisher {

  private final SqsTemplate sqsTemplate;

  @Value("${app.sqs.user-events-queue:user-events}")
  private String userEventsQueue;

  @Override
  public void publish(DomainEvent event) {
    log.info("Publishing {} event to SQS queue {}: {}", event.eventType(), userEventsQueue, event);
    try {
      sqsTemplate.send(userEventsQueue, event);
      log.debug("Published {} event successfully", event.eventType());
    } catch (Exception e) {
      log.error("Failed to publish {} event: {}", event.eventType(), e.getMessage(), e);
      throw e;
    }
  }
}
