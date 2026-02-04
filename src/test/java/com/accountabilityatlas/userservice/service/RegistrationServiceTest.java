package com.accountabilityatlas.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.event.EventPublisher;
import com.accountabilityatlas.userservice.event.UserRegisteredEvent;
import com.accountabilityatlas.userservice.exception.EmailAlreadyExistsException;
import com.accountabilityatlas.userservice.repository.UserRepository;
import com.accountabilityatlas.userservice.repository.UserStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserStatsRepository userStatsRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EventPublisher eventPublisher;

  @InjectMocks private RegistrationService registrationService;

  @Test
  void register_savesUserWithHashedPassword() {
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("SecurePass123")).thenReturn("$2a$12$hashed");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = registrationService.register("test@example.com", "SecurePass123", "TestUser");

    assertThat(result.getEmail()).isEqualTo("test@example.com");
    assertThat(result.getPasswordHash()).isEqualTo("$2a$12$hashed");
    assertThat(result.getDisplayName()).isEqualTo("TestUser");
    assertThat(result.getTrustTier()).isEqualTo(TrustTier.NEW);
    assertThat(result.isEmailVerified()).isFalse();
  }

  @Test
  void register_normalizesEmailToLowercase() {
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = registrationService.register("Test@Example.COM", "SecurePass123", "TestUser");

    assertThat(result.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void register_publishesUserRegisteredEvent() {
    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    registrationService.register("test@example.com", "SecurePass123", "TestUser");

    ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
    verify(eventPublisher).publish(captor.capture());
    assertThat(captor.getValue().email()).isEqualTo("test@example.com");
  }

  @Test
  void register_createsUserStatsWithZeroCounters() {
    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    registrationService.register("test@example.com", "SecurePass123", "TestUser");

    verify(userStatsRepository).save(any());
  }

  @Test
  void register_throwsWhenEmailAlreadyExists() {
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> registrationService.register("taken@example.com", "SecurePass123", "TestUser"))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }
}
