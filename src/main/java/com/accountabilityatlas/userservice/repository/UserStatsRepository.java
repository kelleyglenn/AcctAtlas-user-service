package com.accountabilityatlas.userservice.repository;

import com.accountabilityatlas.userservice.domain.UserStats;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {}
