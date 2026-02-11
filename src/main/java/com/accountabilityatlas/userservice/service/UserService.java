package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.domain.TrustTier;
import com.accountabilityatlas.userservice.domain.User;
import com.accountabilityatlas.userservice.exception.UserNotFoundException;
import com.accountabilityatlas.userservice.repository.UserRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public User getUserById(UUID id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }

  @Transactional
  public User updateTrustTier(UUID id, TrustTier newTier, String reason) {
    User user = getUserById(id);
    TrustTier oldTier = user.getTrustTier();
    user.setTrustTier(newTier);
    User saved = userRepository.save(user);

    log.info("Updated user {} trust tier: {} -> {} (reason: {})", id, oldTier, newTier, reason);

    return saved;
  }
}
