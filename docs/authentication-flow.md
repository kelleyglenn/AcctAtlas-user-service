# User Service - Authentication Flows

## Overview

The User Service implements authentication using stateless JWTs for access control and server-validated refresh tokens for session management.

### Token Strategy

| Token | Lifetime | Storage | Purpose |
|-------|----------|---------|---------|
| Access Token | 15 minutes | Client only (memory/secure storage) | API authorization |
| Refresh Token | 7 days | Hashed in `sessions` table | Obtain new access tokens |

**JWT Structure (Access Token):**

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "trustTier": "TRUSTED",
  "sessionId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "iat": 1704067200,
  "exp": 1704068100
}
```

- Signed with RS256 (asymmetric keys)
- Public key available for other services to validate tokens
- `sessionId` enables server-side revocation checks when needed

### Security Principles

These principles apply throughout all authentication flows:

- **Password hashing:** bcrypt with cost factor 12
- **Token rotation:** Refresh tokens rotated on each use; old token invalidated
- **Rate limiting:** All auth endpoints rate-limited; stricter limits on login and password reset
- **Transport security:** All tokens transmitted over HTTPS only
- **Constant-time comparisons:** Password and token verification use constant-time algorithms to prevent timing attacks

---

## Simple Flows

### Registration (email/password)

1. Client submits `email`, `password`, `displayName`
2. Validate email format, password strength (min 8 chars, complexity rules)
3. Check email not already registered
4. Hash password with bcrypt (cost 12)
5. Create User with `trustTier = NEW`, `emailVerified = false`
6. Create Session, issue access + refresh tokens
7. Publish `UserRegistered` event
8. Send verification email (account functional but unverified)

**Security:** Registration rate-limited by IP (5/hour) to prevent mass account creation. Email existence check uses constant-time comparison to prevent enumeration.

### Login (email/password)

1. Client submits `email`, `password`
2. Find user by email
3. Verify password against stored hash
4. Create Session with device info and IP address
5. Issue access + refresh tokens

**Security:** Return generic "invalid credentials" error whether email doesn't exist or password is wrong (no enumeration). Failed attempts rate-limited: 5 failures triggers 15-minute lockout. Lockout state stored in Redis, not database.

### Token Refresh

1. Client submits refresh token
2. Hash token, find matching Session
3. Verify session not revoked and not expired
4. Rotate: invalidate old refresh token hash, generate new token pair
5. Update Session with new refresh token hash
6. Return new access + refresh tokens

**Security:** Refresh token rotation detects theft. If an attacker steals and uses a refresh token, the legitimate user's next refresh attempt will fail (token already rotated). This triggers detection - the entire session should be revoked.

### Logout

1. Client submits refresh token
2. Find Session by hashed token
3. Set `revokedAt = now` on the Session
4. Return success

**Security:** Access token remains valid until its 15-minute expiry. This is an acceptable tradeoff for stateless validation. For scenarios requiring immediate invalidation, maintain a short-lived Redis blacklist of revoked `sessionId` values that other services check.

---

## OAuth Flow (detailed)

### Sequence Diagram

```
┌──────┐          ┌───────────┐         ┌─────────────┐       ┌──────────┐
│Client│          │API Gateway│         │User Service │       │OAuth Prov│
└──┬───┘          └─────┬─────┘         └──────┬──────┘       └────┬─────┘
   │                    │                      │                   │
   │ GET /auth/oauth/google/url                │                   │
   │───────────────────>│──────────────────────>                   │
   │<───────────────────│ OAuth URL + state    │                   │
   │                    │                      │                   │
   │ Redirect to Provider                      │                   │
   │──────────────────────────────────────────────────────────────>│
   │                    │                      │                   │
   │ User authenticates with provider          │                   │
   │<──────────────────────────────────────────────────────────────│
   │ Redirect with code + state                │                   │
   │                    │                      │                   │
   │ POST /auth/oauth/google {code, state}     │                   │
   │───────────────────>│──────────────────────>                   │
   │                    │                      │                   │
   │                    │                      │ Exchange code     │
   │                    │                      │──────────────────>│
   │                    │                      │<──────────────────│
   │                    │                      │ id_token + profile│
   │                    │                      │                   │
   │                    │    Find/create user  │                   │
   │                    │    Create session    │                   │
   │                    │<─────────────────────│                   │
   │<───────────────────│ Access + refresh     │                   │
   │                    │ tokens               │                   │
└──┴───┘          └─────┴─────┘         └──────┴──────┘       └────┴─────┘
```

### State Parameter

The `state` parameter prevents CSRF attacks and enables account linking:

```json
{
  "csrf": "random-token",
  "linkToUserId": null,
  "exp": 1704067800
}
```

- Signed JWT using server secret
- Expires in 10 minutes
- `linkToUserId` populated when linking OAuth to existing account

**Security:** Always validate `state` signature and expiry before processing OAuth callback. Reject if invalid or expired.

### Google Implementation

1. **Build OAuth URL:**
   - Use Google's OpenID Connect discovery document for endpoints
   - Include scopes: `openid`, `email`, `profile`
   - Include signed `state` parameter

2. **Handle callback:**
   - Validate `state` parameter
   - Exchange `code` for tokens at Google's token endpoint
   - Validate `id_token` signature using Google's public keys (cached in Redis, refreshed daily)
   - Extract claims: `sub` (provider ID), `email`, `name`, `picture`

3. **Find or create user:**
   - Look up OAuthLink by `(GOOGLE, sub)`
   - If found: load existing user
   - If not found: create new User and OAuthLink
   - Create Session, issue tokens

### Apple Implementation

Apple Sign In has specific quirks to handle:

1. **User info only on first auth:**
   - Apple sends `name` and `email` only on the user's first authorization
   - Must capture and store immediately; subsequent auths only provide `sub`
   - If missed, user must revoke app access in Apple settings and re-authorize

2. **Private email relay:**
   - Users can hide their real email
   - Apple provides a relay address like `abc123@privaterelay.appleid.com`
   - Store whatever Apple provides; it forwards to the real address

3. **ID token validation:**
   - Use Apple's public keys from `https://appleid.apple.com/auth/keys`
   - Cache keys in Redis, refresh daily

### Linking OAuth to Existing Account

When a logged-in user wants to add OAuth login to their existing account:

1. User is authenticated (has valid access token)
2. Client requests OAuth URL with `linkToUserId` in state
3. User completes OAuth flow
4. On callback, `state.linkToUserId` is set
5. Verify caller owns that user ID
6. Check if OAuth `providerId` already linked to any account
7. If already linked to different account: reject with error
8. Create OAuthLink connecting provider to existing user

**Security:** The `linkToUserId` check prevents an attacker from linking their OAuth account to a victim's AccountabilityAtlas account. The user must be authenticated as the target account.

---

## Password Reset Flow (detailed)

### Sequence Diagram

```
┌──────┐          ┌─────────────┐       ┌─────────────┐       ┌───────────┐
│Client│          │User Service │       │  Database   │       │Email (SQS)│
└──┬───┘          └──────┬──────┘       └──────┬──────┘       └─────┬─────┘
   │                     │                     │                    │
   │ POST /auth/password/reset {email}         │                    │
   │────────────────────>│                     │                    │
   │                     │ Find user by email  │                    │
   │                     │────────────────────>│                    │
   │                     │<────────────────────│                    │
   │                     │                     │                    │
   │                     │ Generate token      │                    │
   │                     │ Hash + store        │                    │
   │                     │────────────────────>│                    │
   │                     │                     │                    │
   │                     │ Publish PasswordResetRequested           │
   │                     │─────────────────────────────────────────>│
   │<────────────────────│                     │                    │
   │ "Check your email"  │                     │                    │
   │                     │                     │                    │
   ════════════════════════════════════════════════════════════════════
   │                     │                     │                    │
   │ POST /auth/password/reset/confirm         │                    │
   │      {token, newPassword}                 │                    │
   │────────────────────>│                     │                    │
   │                     │ Hash token, lookup  │                    │
   │                     │────────────────────>│                    │
   │                     │<────────────────────│                    │
   │                     │                     │                    │
   │                     │ Validate: not expired, not used         │
   │                     │ Update password_hash│                    │
   │                     │ Mark token used     │                    │
   │                     │ Revoke all sessions │                    │
   │                     │────────────────────>│                    │
   │<────────────────────│                     │                    │
   │ "Password updated"  │                     │                    │
└──┴───┘          └──────┴──────┘       └──────┴──────┘       └─────┴─────┘
```

### Token Generation

1. Generate 32-byte cryptographically random token using `SecureRandom`
2. Compute SHA-256 hash of token
3. Store hash in `password_resets` table (never store raw token)
4. Set expiry to 24 hours from now
5. Build reset URL: `https://app.example.com/reset?token={base64url-encoded-token}`
6. Publish `PasswordResetRequested` event for notification-service

### Reset Completion

1. Receive token and new password from client
2. Hash incoming token with SHA-256
3. Find matching `PasswordReset` record by token hash
4. Validate: `expiresAt > now` and `usedAt IS NULL`
5. Validate new password meets strength requirements
6. Update user's `password_hash` with new bcrypt hash
7. Mark reset token as used: `usedAt = now`
8. Revoke all existing sessions (forces re-login on all devices)
9. Return success

### Security Considerations

- **No enumeration:** Return "If an account exists with this email, you'll receive a reset link" regardless of whether email exists
- **Rate limiting:** 3 reset requests per email per hour
- **Session invalidation:** On successful reset, revoke all sessions. Assumes password was compromised.
- **OAuth-only users:** Reset flow silently no-ops for users without a password (sends no email, returns same generic response). Adding password-based login to an OAuth-only account is a separate authenticated flow, not part of password reset.
- **Token reuse:** Tokens are single-use. The `usedAt` timestamp prevents replay.

---

## Session Management

### Multi-Device Sessions

- Each login creates a new Session record
- Users can have multiple active sessions simultaneously (phone, laptop, tablet)
- `device_info` captured from User-Agent header for display in UI ("Chrome on Windows")
- `ip_address` stored for security review; not enforced by default

### Session Listing

For the "manage sessions" UI:

```java
List<Session> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID userId);
```

Returns all active sessions. UI displays:
- Device info
- IP address (or "Unknown")
- Created date
- Current session highlighted

### Single Session Revocation

User clicks "log out" on a specific session from the list:

1. Verify session belongs to the requesting user
2. Set `revokedAt = now` on that Session
3. That device's next refresh attempt fails, forcing re-login

### Log Out Everywhere

Revokes all sessions for the user, including the current one:

```java
@Modifying
@Query("UPDATE Session s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
int revokeAllForUser(UUID userId, Instant now);
```

**Triggered by:**
- User request (security concern, lost device)
- Password reset completion (automatic)
- Admin action (compromised account)

### Session Cleanup

Expired and revoked sessions accumulate over time. A scheduled cleanup job removes old records:

```java
@Scheduled(cron = "0 0 3 * * *")  // Daily at 3 AM
public void cleanupExpiredSessions() {
    Instant cutoff = Instant.now().minus(Duration.ofDays(7));
    sessionRepository.deleteByExpiresAtBeforeOrRevokedAtBefore(cutoff, cutoff);
}
```

Keeps sessions for 7 days after expiry/revocation for debugging, then purges.
