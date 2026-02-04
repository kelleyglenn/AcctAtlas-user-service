package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.domain.UserStats;
import com.accountabilityatlas.userservice.event.EventPublisher;
import com.accountabilityatlas.userservice.event.UserRegisteredEvent;
import com.accountabilityatlas.userservice.exception.EmailAlreadyExistsException;
import com.accountabilityatlas.userservice.repository.UserRepository;
import com.accountabilityatlas.userservice.repository.UserStatsRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

  private final UserRepository userRepository;
  private final UserStatsRepository userStatsRepository;
  private final PasswordEncoder passwordEncoder;
  private final EventPublisher eventPublisher;

  public RegistrationService(
      UserRepository userRepository,
      UserStatsRepository userStatsRepository,
      PasswordEncoder passwordEncoder,
      EventPublisher eventPublisher) {
    this.userRepository = userRepository;
    this.userStatsRepository = userStatsRepository;
    this.passwordEncoder = passwordEncoder;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public User register(String email, String password, String displayName) {
    String normalizedEmail = email.toLowerCase(Locale.ROOT);

    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyExistsException();
    }

    User user = new User();
    user.setEmail(normalizedEmail);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setDisplayName(displayName);

    user = userRepository.save(user);

    UserStats stats = new UserStats();
    stats.setUser(user);
    userStatsRepository.save(stats);

    eventPublisher.publish(new UserRegisteredEvent(user.getId(), normalizedEmail, Instant.now()));

    return user;
  }
}
