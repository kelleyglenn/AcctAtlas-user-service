# User Service - Trust Tier Logic

## Overview

The trust tier system controls content moderation requirements based on user reputation. Users progress through tiers as they establish trust through quality contributions.

### Trust Tier Definitions

| Tier | Description | Key Privileges |
|------|-------------|----------------|
| NEW | Fresh accounts, unproven | Submissions require moderation approval |
| TRUSTED | Established contributors | Submissions auto-approved, skip moderation queue |
| MODERATOR | Community moderators | Can review and approve/reject items in moderation queue |
| ADMIN | System administrators | Can promote/demote users, system configuration |

### Progression Paths

```
                    ┌──────────┐
                    │   NEW    │
                    └────┬─────┘
                         │ automatic (criteria met)
                         ▼
                    ┌──────────┐
                    │ TRUSTED  │
                    └────┬─────┘
                         │ manual (Admin only)
                         ▼
                    ┌──────────┐
                    │MODERATOR │
                    └────┬─────┘
                         │ manual (Admin only)
                         ▼
                    ┌──────────┐
                    │  ADMIN   │
                    └──────────┘
```

Only NEW → TRUSTED is automatic. All other tier changes require explicit Admin action.

---

## Auto-Promotion: NEW → TRUSTED

### Criteria

A NEW user is eligible for automatic promotion to TRUSTED when ALL of the following are true:

| Criterion | Threshold | Rationale |
|-----------|-----------|-----------|
| Account age | > 30 days | Prevents quick throwaway accounts |
| Approved submissions | ≥ 10 | Demonstrates consistent quality |
| Recent rejections | 0 in last 30 days | No recent quality issues |
| Abuse reports | None open against user | Not under investigation |

### Implementation: Scheduled Job

A Spring `@Scheduled` job runs daily at 2 AM UTC to check for eligible users:

```java
@Service
public class TrustTierPromotionService {

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void promoteEligibleUsers() {
        List<UUID> candidates = userRepository.findPromotionCandidates();

        for (UUID userId : candidates) {
            promoteToTrusted(userId);
        }

        log.info("Trust tier promotion job completed. Promoted {} users.", candidates.size());
    }

    private void promoteToTrusted(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        TrustTier oldTier = user.getTrustTier();

        user.setTrustTier(TrustTier.TRUSTED);
        userRepository.save(user);

        eventPublisher.publish(new TrustTierChangedEvent(
            userId,
            oldTier,
            TrustTier.TRUSTED,
            null,  // changedBy is null for auto-promotion
            "AUTO_PROMOTION"
        ));

        log.info("Auto-promoted user {} from {} to TRUSTED", userId, oldTier);
    }
}
```

**Design decisions:**
- **Daily batch:** Users wait up to 24 hours after meeting criteria. This is acceptable; promotion isn't urgent.
- **Individual transactions:** Each promotion is a separate transaction. A failure on one user doesn't block others.
- **Idempotent:** Running the job multiple times is safe; already-TRUSTED users aren't in the candidate query.

### The Promotion Check Query

```sql
SELECT u.id
FROM users.users u
JOIN users.user_stats s ON u.id = s.user_id
WHERE u.trust_tier = 'NEW'
  -- Account age > 30 days (using sys_period lower bound as creation time)
  AND lower(u.sys_period) < NOW() - INTERVAL '30 days'
  -- At least 10 approved submissions
  AND s.approved_count >= 10
  -- No rejections in last 30 days
  AND NOT EXISTS (
      SELECT 1 FROM content.videos v
      WHERE v.submitted_by = u.id
        AND v.status = 'REJECTED'
        AND lower(v.sys_period) > NOW() - INTERVAL '30 days'
  )
  -- No open abuse reports against this user
  AND NOT EXISTS (
      SELECT 1 FROM moderation.abuse_reports ar
      WHERE ar.content_id = u.id
        AND ar.content_type = 'USER'
        AND ar.status = 'OPEN'
  );
```

**Notes:**
- Uses `lower(sys_period)` for account creation time (temporal table pattern)
- Checks `content.videos` for rejections (cross-schema query)
- Checks `moderation.abuse_reports` for open reports against the user

### Edge Cases

| Scenario | Behavior |
|----------|----------|
| User meets criteria, then gets a rejection before job runs | Not promoted (criteria checked at job runtime) |
| User promoted, then gets rejection next day | Stays TRUSTED (no automatic demotion) |
| User has 10 approved but 1 rejected 31 days ago | Promoted (rejection outside 30-day window) |
| Abuse report opened, then closed before job runs | Promoted (only open reports block) |
| User deleted after meeting criteria | Skipped (findById returns empty) |

---

## Manual Promotion

### Who Can Promote Whom

| Actor | Can Promote To |
|-------|----------------|
| ADMIN | TRUSTED, MODERATOR, ADMIN |
| MODERATOR | (none) |
| TRUSTED | (none) |
| NEW | (none) |

Only Admins can perform manual promotions.

### Constraints

- **No skipping tiers:** NEW cannot be promoted directly to MODERATOR. Must go NEW → TRUSTED → MODERATOR.
- **No self-promotion:** Admins cannot promote themselves to a higher tier (already at max).

### API

```
PUT /users/{id}/trust-tier
Authorization: Bearer <admin-access-token>
Content-Type: application/json

{
  "trustTier": "MODERATOR"
}
```

**Response:** Updated user object or error.

### Implementation

```java
@PutMapping("/users/{id}/trust-tier")
@PreAuthorize("hasRole('ADMIN')")
public UserDto updateTrustTier(
    @PathVariable UUID id,
    @RequestBody UpdateTrustTierRequest request,
    @AuthenticationPrincipal JwtUser admin
) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("User not found"));

    TrustTier currentTier = user.getTrustTier();
    TrustTier newTier = request.getTrustTier();

    validateTierTransition(currentTier, newTier);

    user.setTrustTier(newTier);
    userRepository.save(user);

    eventPublisher.publish(new TrustTierChangedEvent(
        id,
        currentTier,
        newTier,
        admin.getUserId(),
        isPromotion(currentTier, newTier) ? "MANUAL_PROMOTION" : "MANUAL_DEMOTION"
    ));

    return UserDto.from(user);
}

private void validateTierTransition(TrustTier from, TrustTier to) {
    if (from == to) {
        throw new BadRequestException("User is already " + to);
    }

    // Check for tier skipping on promotion
    if (to.ordinal() > from.ordinal() && to.ordinal() - from.ordinal() > 1) {
        throw new BadRequestException("Cannot skip tiers. Promote to " +
            TrustTier.values()[from.ordinal() + 1] + " first.");
    }
}
```

---

## Manual Demotion

### Who Can Demote Whom

| Actor | Can Demote |
|-------|------------|
| ADMIN | TRUSTED → NEW, MODERATOR → TRUSTED, MODERATOR → NEW |

### Constraints

- **Admins cannot demote other Admins:** Prevents power struggles. Admin removal requires database access or super-admin.
- **Admins cannot demote themselves:** Prevents accidental lockout.

### Use Cases

| Scenario | Action |
|----------|--------|
| TRUSTED user submitting low-quality content | Demote to NEW (submissions require moderation again) |
| MODERATOR inactive or misusing privileges | Demote to TRUSTED (loses moderation access) |
| Temporary disciplinary action | Demote, with potential re-promotion later |

### Implementation

Demotion uses the same endpoint as promotion (`PUT /users/{id}/trust-tier`). The `validateTierTransition` method handles demotion validation:

```java
private void validateTierTransition(TrustTier from, TrustTier to) {
    if (from == to) {
        throw new BadRequestException("User is already " + to);
    }

    // Check for tier skipping on promotion
    if (to.ordinal() > from.ordinal() && to.ordinal() - from.ordinal() > 1) {
        throw new BadRequestException("Cannot skip tiers.");
    }

    // Prevent demoting admins
    if (from == TrustTier.ADMIN && to.ordinal() < from.ordinal()) {
        throw new ForbiddenException("Cannot demote administrators.");
    }
}
```

**Note:** Demotion is a soft disciplinary action. For serious violations (spam, abuse, legal issues), account suspension or ban is separate functionality not part of the trust tier system.

---

## Events Published

### TrustTierChanged Event

Published whenever a user's trust tier changes, whether automatic or manual.

```json
{
  "eventType": "TrustTierChanged",
  "timestamp": "2025-06-15T14:30:00Z",
  "payload": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "oldTier": "NEW",
    "newTier": "TRUSTED",
    "changedBy": null,
    "reason": "AUTO_PROMOTION"
  }
}
```

| Field | Description |
|-------|-------------|
| `userId` | The user whose tier changed |
| `oldTier` | Previous trust tier |
| `newTier` | New trust tier |
| `changedBy` | Admin's userId for manual changes; `null` for auto-promotion |
| `reason` | `AUTO_PROMOTION`, `MANUAL_PROMOTION`, or `MANUAL_DEMOTION` |

### Consumers

| Service | Reaction |
|---------|----------|
| moderation-service | Updates moderation rules. TRUSTED+ users' future submissions skip the moderation queue. |
| notification-service | Sends email on promotion to TRUSTED: "Congratulations! Your submissions will now be published immediately." |

### Event Publishing Guarantee

The event is published after the database transaction commits. If event publishing fails:
- The tier change still succeeds (database is source of truth)
- Event is lost (eventual consistency)
- A reconciliation job could detect and republish missed events by comparing database state to event log, if needed

---

## Audit Trail

All tier changes are automatically captured by the temporal table system:

- `users.users_history` contains previous versions of user records
- Each row includes `sys_period` showing when that version was valid
- Query history to see all tier changes for a user:

```sql
SELECT
    trust_tier,
    lower(sys_period) AS effective_from,
    upper(sys_period) AS effective_until
FROM (
    SELECT trust_tier, sys_period FROM users.users WHERE id = $1
    UNION ALL
    SELECT trust_tier, sys_period FROM users.users_history WHERE id = $1
) all_versions
ORDER BY lower(sys_period) DESC;
```

For manual changes, the `TrustTierChanged` event also records `changedBy`, providing attribution not available in the temporal table alone.
