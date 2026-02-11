package com.accountabilityatlas.userservice.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.domain.TrustTier;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

@ExtendWith(MockitoExtension.class)
class SqsEventPublisherTest {

  @Mock private StreamBridge streamBridge;

  private SqsEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new SqsEventPublisher(streamBridge);
  }

  @Test
  void publish_userTrustTierChangedEvent_sendsToStreamBridge() {
    // Arrange
    UserTrustTierChangedEvent event =
        new UserTrustTierChangedEvent(
            UUID.randomUUID(),
            TrustTier.NEW,
            TrustTier.TRUSTED,
            UserTrustTierChangedEvent.ChangeReason.AUTO_PROMOTION,
            Instant.now());
    when(streamBridge.send(eq("userEvent-out-0"), any())).thenReturn(true);

    // Act
    publisher.publish(event);

    // Assert
    verify(streamBridge).send(eq("userEvent-out-0"), eq(event));
  }

  @Test
  void publish_userRegisteredEvent_sendsToStreamBridge() {
    // Arrange
    UserRegisteredEvent event =
        new UserRegisteredEvent(UUID.randomUUID(), "test@example.com", Instant.now());
    when(streamBridge.send(eq("userEvent-out-0"), any())).thenReturn(true);

    // Act
    publisher.publish(event);

    // Assert
    verify(streamBridge).send(eq("userEvent-out-0"), eq(event));
  }
}
