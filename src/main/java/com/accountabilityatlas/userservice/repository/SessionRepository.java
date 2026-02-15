package com.accountabilityatlas.userservice.repository;

import com.accountabilityatlas.userservice.domain.Session;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SessionRepository extends JpaRepository<Session, UUID> {

  @Query(
      "SELECT s FROM Session s WHERE s.refreshTokenHash = :hash AND s.revokedAt IS NULL AND"
          + " s.expiresAt > :now")
  Optional<Session> findValidByRefreshTokenHash(String hash, Instant now);

  @Modifying
  @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
  int revokeAllForUser(UUID userId, Instant now);

  @Modifying
  @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.id = :sessionId AND s.revokedAt IS NULL")
  int revokeById(UUID sessionId, Instant now);
}
