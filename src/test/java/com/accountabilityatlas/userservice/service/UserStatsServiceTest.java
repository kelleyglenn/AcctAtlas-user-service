package com.accountabilityatlas.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.domain.UserStats;
import com.accountabilityatlas.userservice.repository.UserStatsRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceTest {

  @Mock private UserStatsRepository userStatsRepository;
  @InjectMocks private UserStatsService userStatsService;

  private UserStats createStats(UUID userId, int submissions, int approved, int rejected) {
    UserStats stats = new UserStats();
    stats.setUserId(userId);
    stats.setSubmissionCount(submissions);
    stats.setApprovedCount(approved);
    stats.setRejectedCount(rejected);
    stats.setUpdatedAt(Instant.now());
    return stats;
  }

  @Test
  void incrementSubmissionCount_userExists_incrementsByOne() {
    UUID userId = UUID.randomUUID();
    UserStats stats = createStats(userId, 5, 3, 1);
    when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

    userStatsService.incrementSubmissionCount(userId);

    ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(captor.capture());
    assertThat(captor.getValue().getSubmissionCount()).isEqualTo(6);
  }

  @Test
  void incrementSubmissionCount_userNotFound_doesNothing() {
    UUID userId = UUID.randomUUID();
    when(userStatsRepository.findById(userId)).thenReturn(Optional.empty());

    userStatsService.incrementSubmissionCount(userId);

    verify(userStatsRepository).findById(userId);
  }

  @Test
  void handleStatusChange_toApproved_incrementsApprovedCount() {
    UUID userId = UUID.randomUUID();
    UserStats stats = createStats(userId, 5, 3, 1);
    when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

    userStatsService.handleStatusChange(userId, "PENDING", "APPROVED");

    ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(captor.capture());
    assertThat(captor.getValue().getApprovedCount()).isEqualTo(4);
    assertThat(captor.getValue().getRejectedCount()).isEqualTo(1);
  }

  @Test
  void handleStatusChange_fromApproved_decrementsApprovedCount() {
    UUID userId = UUID.randomUUID();
    UserStats stats = createStats(userId, 5, 3, 1);
    when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

    userStatsService.handleStatusChange(userId, "APPROVED", "REJECTED");

    ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(captor.capture());
    assertThat(captor.getValue().getApprovedCount()).isEqualTo(2);
    assertThat(captor.getValue().getRejectedCount()).isEqualTo(2);
  }

  @Test
  void handleStatusChange_toRejected_incrementsRejectedCount() {
    UUID userId = UUID.randomUUID();
    UserStats stats = createStats(userId, 5, 3, 0);
    when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

    userStatsService.handleStatusChange(userId, "PENDING", "REJECTED");

    ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(captor.capture());
    assertThat(captor.getValue().getApprovedCount()).isEqualTo(3);
    assertThat(captor.getValue().getRejectedCount()).isEqualTo(1);
  }

  @Test
  void handleStatusChange_fromRejected_decrementsRejectedCount() {
    UUID userId = UUID.randomUUID();
    UserStats stats = createStats(userId, 5, 3, 2);
    when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

    userStatsService.handleStatusChange(userId, "REJECTED", "APPROVED");

    ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(captor.capture());
    assertThat(captor.getValue().getApprovedCount()).isEqualTo(4);
    assertThat(captor.getValue().getRejectedCount()).isEqualTo(1);
  }

  @Test
  void handleStatusChange_approvedCountFloorAtZero() {
    UUID userId = UUID.randomUUID();
    UserStats stats = createStats(userId, 1, 0, 0);
    when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

    userStatsService.handleStatusChange(userId, "APPROVED", "REJECTED");

    ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(captor.capture());
    assertThat(captor.getValue().getApprovedCount()).isEqualTo(0);
    assertThat(captor.getValue().getRejectedCount()).isEqualTo(1);
  }

  @Test
  void handleStatusChange_rejectedCountFloorAtZero() {
    UUID userId = UUID.randomUUID();
    UserStats stats = createStats(userId, 1, 0, 0);
    when(userStatsRepository.findById(userId)).thenReturn(Optional.of(stats));

    userStatsService.handleStatusChange(userId, "REJECTED", "PENDING");

    ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
    verify(userStatsRepository).save(captor.capture());
    assertThat(captor.getValue().getRejectedCount()).isEqualTo(0);
  }

  @Test
  void handleStatusChange_userNotFound_doesNothing() {
    UUID userId = UUID.randomUUID();
    when(userStatsRepository.findById(userId)).thenReturn(Optional.empty());

    userStatsService.handleStatusChange(userId, "PENDING", "APPROVED");

    verify(userStatsRepository).findById(userId);
  }
}
