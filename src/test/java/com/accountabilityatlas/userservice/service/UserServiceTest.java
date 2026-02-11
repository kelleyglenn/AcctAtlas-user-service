package com.accountabilityatlas.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.event.EventPublisher;
import com.accountabilityatlas.userservice.event.UserTrustTierChangedEvent;
import com.accountabilityatlas.userservice.exception.UserNotFoundException;
import com.accountabilityatlas.userservice.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private EventPublisher eventPublisher;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, eventPublisher);
  }

  @Test
  void getUserById_returnsUser_whenFound() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    User result = userService.getUserById(userId);

    assertThat(result).isEqualTo(user);
  }

  @Test
  void getUserById_throwsException_whenNotFound() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(userId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(userId.toString());
  }

  @Test
  void updateTrustTier_validTier_updatesTierAndPublishesEvent() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.NEW);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = userService.updateTrustTier(userId, TrustTier.TRUSTED, "AUTO_PROMOTION");

    assertThat(result.getTrustTier()).isEqualTo(TrustTier.TRUSTED);
    verify(userRepository).save(user);

    // Verify event was published
    ArgumentCaptor<UserTrustTierChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(UserTrustTierChangedEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());

    UserTrustTierChangedEvent event = eventCaptor.getValue();
    assertThat(event.userId()).isEqualTo(userId);
    assertThat(event.oldTier()).isEqualTo(TrustTier.NEW);
    assertThat(event.newTier()).isEqualTo(TrustTier.TRUSTED);
    assertThat(event.reason()).isEqualTo(UserTrustTierChangedEvent.ChangeReason.AUTO_PROMOTION);
  }

  @Test
  void updateTrustTier_sameTier_doesNotPublishEvent() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setEmail("test@example.com");
    user.setDisplayName("TestUser");
    user.setTrustTier(TrustTier.TRUSTED);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    User result = userService.updateTrustTier(userId, TrustTier.TRUSTED, "MANUAL");

    assertThat(result.getTrustTier()).isEqualTo(TrustTier.TRUSTED);
    verify(userRepository, never()).save(any());
    verify(eventPublisher, never()).publish(any());
  }

  @Test
  void updateTrustTier_userNotFound_throwsException() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateTrustTier(userId, TrustTier.TRUSTED, "reason"))
        .isInstanceOf(UserNotFoundException.class);
  }
}
