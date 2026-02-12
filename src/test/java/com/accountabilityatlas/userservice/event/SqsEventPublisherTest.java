package com.accountabilityatlas.userservice.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.accountabilityatlas.userservice.domain.TrustTier;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SqsEventPublisherTest {

  private static final String USER_EVENTS_QUEUE = "user-events";

  @Mock private SqsTemplate sqsTemplate;

  @InjectMocks private SqsEventPublisher publisher;

  @Test
  void publish_userTrustTierChangedEvent_sendsToSqs() {
    // Arrange
    ReflectionTestUtils.setField(publisher, "userEventsQueue", USER_EVENTS_QUEUE);
    UserTrustTierChangedEvent event =
        new UserTrustTierChangedEvent(
            UUID.randomUUID(),
            TrustTier.NEW,
            TrustTier.TRUSTED,
            UserTrustTierChangedEvent.ChangeReason.AUTO_PROMOTION,
            Instant.now());

    // Act
    publisher.publish(event);

    // Assert
    verify(sqsTemplate).send(eq(USER_EVENTS_QUEUE), eq(event));
  }

  @Test
  void publish_userRegisteredEvent_sendsToSqs() {
    // Arrange
    ReflectionTestUtils.setField(publisher, "userEventsQueue", USER_EVENTS_QUEUE);
    UserRegisteredEvent event =
        new UserRegisteredEvent(UUID.randomUUID(), "test@example.com", Instant.now());

    // Act
    publisher.publish(event);

    // Assert
    verify(sqsTemplate).send(eq(USER_EVENTS_QUEUE), eq(event));
  }
}
