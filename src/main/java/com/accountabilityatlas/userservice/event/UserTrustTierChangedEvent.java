package com.accountabilityatlas.userservice.event;

import com.accountabilityatlas.userservice.domain.TrustTier;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user's trust tier changes.
 *
 * @param userId the user whose tier changed
 * @param oldTier the previous trust tier
 * @param newTier the new trust tier
 * @param reason the reason for the change
 * @param timestamp when the change occurred
 */
public record UserTrustTierChangedEvent(
    UUID userId, TrustTier oldTier, TrustTier newTier, ChangeReason reason, Instant timestamp)
    implements DomainEvent {

  public enum ChangeReason {
    AUTO_PROMOTION,
    AUTO_DEMOTION,
    MANUAL
  }

  @Override
  public String eventType() {
    return "UserTrustTierChanged";
  }
}
