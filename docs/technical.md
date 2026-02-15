# User Service - Technical Documentation

## Service Overview

The User Service manages identity and access management for AccountabilityAtlas. It handles user registration, authentication, profile management, and the trust tier system that controls content moderation requirements.

## Responsibilities

- User registration (email/password)
- OAuth 2.0 authentication (Google, Apple)
- JWT token issuance and refresh
- User profile CRUD operations
- Trust tier management and progression
- Password reset flow
- Session management and revocation

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.4.x |
| Language | Java 21 |
| Build | Gradle |
| Database | PostgreSQL 15 |
| Cache | Redis |
| Security | Spring Security 6 |

## Dependencies

- **PostgreSQL**: User accounts, OAuth links, sessions
- **Redis**: Session cache, rate limiting
- **SQS**: Event publishing (UserRegistered, TrustTierChanged)

## Documentation Index

| Document | Status | Description |
|----------|--------|-------------|
| [api-specification.yaml](api-specification.yaml) | Complete | OpenAPI 3.1 specification |
| [database-schema.md](database-schema.md) | Complete | JPA entity mappings, indexes, query patterns |
| [authentication-flow.md](authentication-flow.md) | Complete | OAuth, password reset, session management |
| [trust-tier-logic.md](trust-tier-logic.md) | Complete | Auto-promotion logic, manual tier changes |

## Domain Model

```
User (temporal - sys_period tracks history)
├── id: UUID
├── email: String
├── emailVerified: boolean (default: false)
├── passwordHash: String (nullable for OAuth-only)
├── displayName: String
├── avatarUrl: String (nullable)
├── trustTier: TrustTier (NEW, TRUSTED, MODERATOR, ADMIN)
└── sysPeriod: tstzrange  // lower bound = created, NULL upper = current

UserStats (non-temporal - counters change frequently)
├── userId: UUID
├── submissionCount: int
├── approvedCount: int
├── rejectedCount: int
└── updatedAt: Instant

OAuthLink (temporal - sys_period tracks history)
├── userId: UUID
├── provider: OAuthProvider (GOOGLE, APPLE)
├── providerId: String
└── sysPeriod: tstzrange  // lower bound = linked time

Session (non-temporal - transient, high churn)
├── id: UUID
├── userId: UUID
├── refreshTokenHash: String
├── deviceInfo: String (nullable)
├── ipAddress: String (nullable, max 45 chars for IPv6)
├── createdAt: Instant
├── expiresAt: Instant
└── revokedAt: Instant (nullable)

PasswordReset (non-temporal - transient, expires in 24 hours)
├── id: UUID
├── userId: UUID
├── tokenHash: String
├── createdAt: Instant
├── expiresAt: Instant
└── usedAt: Instant (nullable)
```

## JWT and Key Management

The user-service is the **sole issuer of JWTs** in the system. It signs access tokens with an RSA private key (RS256) and exposes the corresponding public key via a standard JWKS endpoint, enabling downstream services to validate tokens independently.

### JWKS Endpoint

`GET /.well-known/jwks.json` — Returns the RSA public key in [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517) JWK Set format. No authentication required.

Downstream services (video-service, moderation-service, etc.) configure Spring's OAuth2 Resource Server to fetch the public key:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://user-service:8081/.well-known/jwks.json
```

Spring's `NimbusJwtDecoder` fetches and caches the key set automatically, refreshing when needed.

### Current Limitations

- RSA key pair is generated dynamically at startup (`JwtConfig.java`). Restarting user-service invalidates all existing tokens.
- For production: load keys from AWS Secrets Manager with rotation and `kid` header support. See [remaining-work.md](remaining-work.md).

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /.well-known/jwks.json | Public | JWKS endpoint (RSA public key) |
| POST | /auth/register | Public | Create account |
| POST | /auth/login | Public | Email/password login |
| POST | /auth/oauth/{provider} | Public | OAuth login |
| POST | /auth/refresh | Refresh | Refresh access token |
| POST | /auth/logout | User | Invalidate session |
| POST | /auth/password/reset | Public | Request password reset |
| POST | /auth/password/reset/confirm | Public | Complete reset |
| GET | /users/me | User | Get current profile |
| PUT | /users/me | User | Update profile |
| GET | /users/{id} | User | Get public profile |
| PUT | /users/{id}/trust-tier | Admin | Update trust tier |

## Events Published

| Event | Payload | Consumers |
|-------|---------|-----------|
| UserRegistered | userId, email, timestamp | notification-service |
| TrustTierChanged | userId, oldTier, newTier, changedBy | moderation-service |

## Trust Tier Progression

```
NEW → TRUSTED (automatic):
  - Account age > 30 days
  - >= 10 approved submissions
  - 0 rejected submissions in last 30 days
  - No active abuse reports against user

TRUSTED → MODERATOR:
  - Manual promotion by Admin only

MODERATOR → ADMIN:
  - Manual designation only
```

## Local Development

```bash
# Start dependencies
docker-compose up -d postgres redis

# Run migrations
./gradlew flywayMigrate

# Run service
./gradlew bootRun

# Service available at http://localhost:8081
```
