package com.accountabilityatlas.userservice.event;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record VideoSubmittedEvent(
    UUID videoId,
    UUID submitterId,
    String submitterTrustTier,
    String title,
    Set<String> amendments,
    List<UUID> locationIds,
    Instant timestamp) {}
