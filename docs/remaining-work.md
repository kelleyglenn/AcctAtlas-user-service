# Remaining Work

Tracks stubs, temporary measures, and unimplemented features after the initial project setup (PR #2, issue #1).

## Controller Stubs (return 501 Not Implemented)

These endpoints have generated OpenAPI interfaces but no implementation beyond a 501 stub in `AuthController`.

| Endpoint | Description | Notes |
|----------|-------------|-------|
| `POST /auth/logout` | Revoke current session | `SessionRepository.revokeAllForUser()` already exists |
| `POST /auth/oauth/{provider}` | Google/Apple OAuth login | See `docs/authentication-flow.md` for flow details |
| `POST /auth/refresh` | Refresh token rotation | `SessionRepository.findValidByRefreshTokenHash()` already exists |
| `POST /auth/password/reset` | Request password reset email | Use 1-hour token expiry. `PasswordReset` entity exists |
| `POST /auth/password/reset/confirm` | Complete password reset | Validate token, update password hash |

## Users and Admin API

`UsersController` implements `UsersApi`. `AdminApi` has no controller yet.

| Endpoint | Status | Description |
|----------|--------|-------------|
| `GET /users/me` | **Done** | Get current user profile |
| `PUT /users/me` | Stub (501) | Update own profile |
| `GET /users/{id}` | Stub (501) | Get public user profile |
| `PUT /users/{id}/trust-tier` | Not started | Admin: update user trust tier |

## Temporary Measures

| Item | Current State | Target State |
|------|---------------|--------------|
| JaCoCo threshold | 0.80 in `build.gradle` | Done |
| RSA key pair | Generated at startup (`JwtConfig.java`) | Load from AWS Secrets Manager; add key rotation and `kid` header |
| Integration test Redis | Excluded via `spring.autoconfigure.exclude` | Add Redis TestContainer or mock |
| `springdoc` version | 2.8.14 (Spring Boot 3.x compatible) | Upgrade to 3.x when migrating to Spring Boot 4.x |

## Not Yet Started

| Feature | Reference |
|---------|-----------|
| Rate limiting | `docs/authentication-flow.md`, `../../docs/06-SecurityArchitecture.md` |
| Email verification flow | `docs/authentication-flow.md` |
| Trust tier auto-promotion (NEW -> TRUSTED) | `docs/trust-tier-logic.md` |
| Domain event publishing to SQS | Replace `LoggingEventPublisher` with SQS implementation |

## Recently Completed

| Feature | PR | Notes |
|---------|-----|-------|
| JWT validation filter | #4 | `JwtAuthenticationFilter` validates Bearer tokens; `SecurityConfig` protects `/users/**` |
| `GET /users/me` endpoint | #4 | `UsersController` returns authenticated user's profile |
