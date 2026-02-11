package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.event.EventPublisher;
import com.accountabilityatlas.userservice.event.UserTrustTierChangedEvent;
import com.accountabilityatlas.userservice.exception.UserNotFoundException;
import com.accountabilityatlas.userservice.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final EventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public User getUserById(UUID id) {
    return getUserByIdInternal(id);
  }

  @Transactional
  public User updateTrustTier(UUID id, TrustTier newTier, String reason) {
    User user = getUserByIdInternal(id);
    TrustTier oldTier = user.getTrustTier();

    if (oldTier == newTier) {
      log.debug("User {} already has trust tier {}, no change needed", id, newTier);
      return user;
    }

    user.setTrustTier(newTier);
    User saved = userRepository.save(user);

    log.info("Updated user {} trust tier: {} -> {} (reason: {})", id, oldTier, newTier, reason);

    // Publish event to notify other services
    UserTrustTierChangedEvent.ChangeReason changeReason = mapReason(reason);
    eventPublisher.publish(
        new UserTrustTierChangedEvent(id, oldTier, newTier, changeReason, Instant.now()));

    return saved;
  }

  private UserTrustTierChangedEvent.ChangeReason mapReason(String reason) {
    if (reason == null) {
      return UserTrustTierChangedEvent.ChangeReason.MANUAL;
    }
    return switch (reason.toUpperCase(Locale.ROOT)) {
      case "AUTO_PROMOTION" -> UserTrustTierChangedEvent.ChangeReason.AUTO_PROMOTION;
      case "AUTO_DEMOTION" -> UserTrustTierChangedEvent.ChangeReason.AUTO_DEMOTION;
      default -> UserTrustTierChangedEvent.ChangeReason.MANUAL;
    };
  }

  private User getUserByIdInternal(UUID id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}
