package com.accountabilityatlas.userservice.repository;

import com.accountabilityatlas.userservice.domain.UserPrivacySettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettings, UUID> {}
