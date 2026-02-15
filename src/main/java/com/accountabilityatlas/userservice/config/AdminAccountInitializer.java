package com.accountabilityatlas.userservice.config;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.domain.UserStats;
import com.accountabilityatlas.userservice.repository.UserRepository;
import com.accountabilityatlas.userservice.repository.UserStatsRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

  private final UserRepository userRepository;
  private final UserStatsRepository userStatsRepository;
  private final String adminEmail;
  private final String adminPasswordHash;

  public AdminAccountInitializer(
      UserRepository userRepository,
      UserStatsRepository userStatsRepository,
      @Value("${ADMIN_EMAIL:}") String adminEmail,
      @Value("${ADMIN_PASSWORD_HASH:}") String adminPasswordHash) {
    this.userRepository = userRepository;
    this.userStatsRepository = userStatsRepository;
    this.adminEmail = adminEmail;
    this.adminPasswordHash = adminPasswordHash;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (adminEmail.isEmpty() || adminPasswordHash.isEmpty()) {
      log.info("ADMIN_EMAIL or ADMIN_PASSWORD_HASH not set; skipping admin account seeding");
      return;
    }

    if (userRepository.existsByEmail(adminEmail)) {
      log.info("Admin account already exists for email: {}", adminEmail);
      return;
    }

    User admin = new User();
    admin.setEmail(adminEmail);
    admin.setPasswordHash(adminPasswordHash);
    admin.setDisplayName("Admin");
    admin.setTrustTier(TrustTier.ADMIN);
    admin.setEmailVerified(true);
    admin = userRepository.save(admin);

    UserStats stats = new UserStats();
    stats.setUser(admin);
    stats.setUpdatedAt(Instant.now());
    userStatsRepository.save(stats);

    log.info("Admin account created for email: {}", adminEmail);
  }
}
