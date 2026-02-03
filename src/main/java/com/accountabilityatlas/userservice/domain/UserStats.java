package com.accountabilityatlas.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_stats", schema = "users")
public class UserStats {

  @Id private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "submission_count", nullable = false)
  private int submissionCount = 0;

  @Column(name = "approved_count", nullable = false)
  private int approvedCount = 0;

  @Column(name = "rejected_count", nullable = false)
  private int rejectedCount = 0;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public int getSubmissionCount() {
    return submissionCount;
  }

  public void setSubmissionCount(int submissionCount) {
    this.submissionCount = submissionCount;
  }

  public int getApprovedCount() {
    return approvedCount;
  }

  public void setApprovedCount(int approvedCount) {
    this.approvedCount = approvedCount;
  }

  public int getRejectedCount() {
    return rejectedCount;
  }

  public void setRejectedCount(int rejectedCount) {
    this.rejectedCount = rejectedCount;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
