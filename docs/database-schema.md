# User Service - Database Schema

## Overview

This document describes the database schema for the User Service, focusing on JPA entity mappings and service-specific implementation details.

**Authoritative SQL Schema:** See [05-DataArchitecture.md](../../AcctAtlas-HighLevelDocumentation/05-DataArchitecture.md) for complete SQL definitions, including table creation statements, constraints, and indexes.

### Tables Owned by User Service

| Table | Temporal | Description |
|-------|----------|-------------|
| `users.users` | Yes | Core user accounts |
| `users.users_history` | - | Automatic history for users |
| `users.user_stats` | No | Submission counters |
| `users.oauth_links` | Yes | OAuth provider connections |
| `users.oauth_links_history` | - | Automatic history for OAuth links |
| `users.sessions` | No | Active refresh token sessions |
| `users.password_resets` | No | Password reset tokens |

The service uses Spring Data JPA with custom handling for PostgreSQL's `tstzrange` temporal columns.

---

## JPA Entity Mappings

### User Entity

```java
@Entity
@Table(name = "users", schema = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "password_hash")
    private String passwordHash;  // NULL for OAuth-only users

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_tier", nullable = false, length = 20)
    private TrustTier trustTier = TrustTier.NEW;

    // Managed by PostgreSQL trigger - read-only in JPA
    @Column(name = "sys_period", insertable = false, updatable = false)
    @Type(TstzRangeType.class)
    private TstzRange sysPeriod;

    // Derived from sys_period lower bound
    @Formula("lower(sys_period)")
    private Instant createdAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserStats stats;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OAuthLink> oauthLinks;
}
```

**Notes:**
- `sysPeriod` is read-only; PostgreSQL's `versioning` trigger manages it automatically
- `createdAt` uses `@Formula` to extract the lower bound of `sys_period` without storing it separately
- `passwordHash` is nullable to support OAuth-only accounts

### TrustTier Enum

```java
public enum TrustTier {
    NEW,       // Fresh accounts, submissions require moderation
    TRUSTED,   // Established contributors, submissions auto-approved
    MODERATOR, // Can review moderation queue
    ADMIN      // System administrators
}
```

### UserStats Entity

```java
@Entity
@Table(name = "user_stats", schema = "users")
public class UserStats {

    @Id
    private UUID userId;

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

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

**Notes:**
- Uses `@MapsId` for shared primary key with User
- `@UpdateTimestamp` automatically sets `updatedAt` on every save
- Non-temporal: counters change frequently, history would explode storage

### OAuthLink Entity

```java
@Entity
@Table(name = "oauth_links", schema = "users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
public class OAuthLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "sys_period", insertable = false, updatable = false)
    @Type(TstzRangeType.class)
    private TstzRange sysPeriod;

    @Formula("lower(sys_period)")
    private Instant linkedAt;
}
```

### OAuthProvider Enum

```java
public enum OAuthProvider {
    GOOGLE,
    APPLE
}
```

### Session Entity

```java
@Entity
@Table(name = "sessions", schema = "users")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "ip_address")
    @Type(InetAddressType.class)
    private InetAddress ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isValid(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
```

**Notes:**
- Non-temporal: sessions are transient with high churn
- `ipAddress` uses custom Hibernate type for PostgreSQL's `INET` column type
- `isValid()` helper encapsulates the validity check logic

### PasswordReset Entity

```java
@Entity
@Table(name = "password_resets", schema = "users")
public class PasswordReset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;  // SHA-256 hash, never store raw token

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isValid(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
```

**Notes:**
- Non-temporal: tokens expire in 24 hours, no long-term audit value
- Always store hashed tokens, never raw values

---

## Temporal vs Non-Temporal Decisions

| Table | Temporal | Rationale |
|-------|----------|-----------|
| `users` | Yes | Audit trail for profile changes; trust tier history needed for disputes |
| `oauth_links` | Yes | Track when OAuth was linked/unlinked for security audits |
| `user_stats` | No | Counters change on every submission - history would explode storage |
| `sessions` | No | Transient by nature, high churn, no audit value |
| `password_resets` | No | Expires in 24h, no long-term value |

**Storage implications:** Temporal tables roughly double write I/O and storage for tracked tables. History tables are append-only and grow indefinitely until archived.

**Accessing history:** History tables (`users_history`, `oauth_links_history`) are for manual SQL auditing only. The application does not expose history queries through APIs or services.

---

## Index Strategy

| Index | Column(s) | Purpose |
|-------|-----------|---------|
| `idx_users_email` | `email` | Login lookup, uniqueness enforcement |
| `idx_users_trust_tier` | `trust_tier` | Admin queries filtering by tier, auto-promotion candidate scans |
| `idx_oauth_links_user` | `user_id` | Find all OAuth links for a user (profile page, unlinking) |
| `idx_sessions_user` | `user_id` | List active sessions, "log out everywhere" feature |
| `idx_sessions_expires` | `expires_at` | Cleanup job finding expired sessions |
| `idx_password_resets_token` | `token_hash` | Token validation during password reset |

**Guidance:** Don't add indexes speculatively. Each index slows writes and consumes storage. Add only when query patterns demand it.

---

## Common Query Patterns

### Find user by email (login)

```java
Optional<User> findByEmail(String email);
```

Uses `idx_users_email`. Note: case-sensitivity depends on database collation.

### Check if OAuth provider already linked

```java
boolean existsByProviderAndProviderId(OAuthProvider provider, String providerId);
```

Uses the unique constraint index on `(provider, provider_id)`.

### Validate session

```java
@Query("SELECT s FROM Session s WHERE s.id = :id AND s.revokedAt IS NULL AND s.expiresAt > :now")
Optional<Session> findValidSession(UUID id, Instant now);
```

### Revoke all sessions for user ("log out everywhere")

```java
@Modifying
@Query("UPDATE Session s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
int revokeAllForUser(UUID userId, Instant now);
```

Uses `idx_sessions_user`.

### Find promotion candidates (trust tier)

```java
@Query(nativeQuery = true, value = """
    SELECT u.id FROM users.users u
    JOIN users.user_stats s ON u.id = s.user_id
    WHERE u.trust_tier = 'NEW'
      AND lower(u.sys_period) < NOW() - INTERVAL '30 days'
      AND s.approved_count >= 10
      AND NOT EXISTS (
          SELECT 1 FROM content.videos v
          WHERE v.submitted_by = u.id
            AND v.status = 'REJECTED'
            AND lower(v.sys_period) > NOW() - INTERVAL '30 days'
      )
      AND NOT EXISTS (
          SELECT 1 FROM moderation.abuse_reports ar
          WHERE ar.content_id = u.id
            AND ar.content_type = 'USER'
            AND ar.status = 'OPEN'
      )
    """)
List<UUID> findPromotionCandidates();
```

See [trust-tier-logic.md](trust-tier-logic.md) for full auto-promotion documentation.

---

## Migration Notes

- **Flyway naming:** `V{version}__{description}.sql` (e.g., `V001__create_users_schema.sql`)
- **Temporal table changes:** When adding columns to temporal tables, add to both main and history tables in the same migration
- **Backfilling data:** Use `sys_period` lower bound as effective date; don't add separate `created_at` columns
- **Testing migrations:** Run `./gradlew flywayMigrate` against local PostgreSQL before committing
