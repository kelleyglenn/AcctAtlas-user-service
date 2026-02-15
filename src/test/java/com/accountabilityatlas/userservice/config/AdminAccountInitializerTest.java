package com.accountabilityatlas.userservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.domain.UserStats;
import com.accountabilityatlas.userservice.repository.UserRepository;
import com.accountabilityatlas.userservice.repository.UserStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

  @Mock private UserRepository userRepository;
  @Mock private UserStatsRepository userStatsRepository;

  @Test
  void run_createsAdminWhenEnvVarsSetAndUserDoesNotExist() {
    // Arrange
    String email = "admin@example.com";
    String passwordHash = "$2a$12$hashedpassword";
    AdminAccountInitializer initializer =
        new AdminAccountInitializer(userRepository, userStatsRepository, email, passwordHash);
    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    initializer.run(new DefaultApplicationArguments());

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getEmail()).isEqualTo(email);
    assertThat(savedUser.getPasswordHash()).isEqualTo(passwordHash);
    assertThat(savedUser.getTrustTier()).isEqualTo(TrustTier.ADMIN);
    assertThat(savedUser.isEmailVerified()).isTrue();
    assertThat(savedUser.getDisplayName()).isEqualTo("Admin");

    ArgumentCaptor<UserStats> statsCaptor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(statsCaptor.capture());
    assertThat(statsCaptor.getValue().getUpdatedAt()).isNotNull();
  }

  @Test
  void run_skipsWhenUserAlreadyExists() {
    // Arrange
    String email = "admin@example.com";
    String passwordHash = "$2a$12$hashedpassword";
    AdminAccountInitializer initializer =
        new AdminAccountInitializer(userRepository, userStatsRepository, email, passwordHash);
    when(userRepository.existsByEmail(email)).thenReturn(true);

    // Act
    initializer.run(new DefaultApplicationArguments());

    // Assert
    verify(userRepository, never()).save(any(User.class));
    verify(userStatsRepository, never()).save(any(UserStats.class));
  }

  @Test
  void run_skipsWhenEnvVarsMissing() {
    // Arrange
    AdminAccountInitializer initializer =
        new AdminAccountInitializer(userRepository, userStatsRepository, "", "");

    // Act
    initializer.run(new DefaultApplicationArguments());

    // Assert
    verify(userRepository, never()).existsByEmail(any());
    verify(userRepository, never()).save(any(User.class));
    verify(userStatsRepository, never()).save(any(UserStats.class));
  }

  @Test
  void run_skipsWhenOnlyEmailSet() {
    // Arrange
    AdminAccountInitializer initializer =
        new AdminAccountInitializer(userRepository, userStatsRepository, "admin@example.com", "");

    // Act
    initializer.run(new DefaultApplicationArguments());

    // Assert
    verify(userRepository, never()).existsByEmail(any());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void run_skipsWhenOnlyPasswordHashSet() {
    // Arrange
    AdminAccountInitializer initializer =
        new AdminAccountInitializer(
            userRepository, userStatsRepository, "", "$2a$12$hashedpassword");

    // Act
    initializer.run(new DefaultApplicationArguments());

    // Assert
    verify(userRepository, never()).existsByEmail(any());
    verify(userRepository, never()).save(any(User.class));
  }
}
