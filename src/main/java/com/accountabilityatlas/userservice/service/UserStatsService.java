package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.repository.UserStatsRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatsService {

  private final UserStatsRepository userStatsRepository;

  @Transactional
  public void incrementSubmissionCount(UUID userId) {
    userStatsRepository
        .findById(userId)
        .ifPresentOrElse(
            stats -> {
              stats.setSubmissionCount(stats.getSubmissionCount() + 1);
              stats.setUpdatedAt(Instant.now());
              userStatsRepository.save(stats);
              log.debug("Incremented submission count for user {}", userId);
            },
            () -> log.warn("UserStats not found for user {}, skipping submission count", userId));
  }

  @Transactional
  public void handleStatusChange(UUID userId, String previousStatus, String newStatus) {
    userStatsRepository
        .findById(userId)
        .ifPresentOrElse(
            stats -> {
              if ("APPROVED".equals(newStatus)) {
                stats.setApprovedCount(stats.getApprovedCount() + 1);
              } else if ("APPROVED".equals(previousStatus)) {
                stats.setApprovedCount(Math.max(0, stats.getApprovedCount() - 1));
              }

              if ("REJECTED".equals(newStatus)) {
                stats.setRejectedCount(stats.getRejectedCount() + 1);
              } else if ("REJECTED".equals(previousStatus)) {
                stats.setRejectedCount(Math.max(0, stats.getRejectedCount() - 1));
              }

              stats.setUpdatedAt(Instant.now());
              userStatsRepository.save(stats);
              log.debug(
                  "Updated stats for user {} ({} -> {}): approved={}, rejected={}",
                  userId,
                  previousStatus,
                  newStatus,
                  stats.getApprovedCount(),
                  stats.getRejectedCount());
            },
            () -> log.warn("UserStats not found for user {}, skipping status change", userId));
  }
}
