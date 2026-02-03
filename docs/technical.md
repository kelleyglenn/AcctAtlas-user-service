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
├── ipAddress: InetAddress (nullable)
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

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
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
