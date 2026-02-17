package com.accountabilityatlas.userservice.repository;

import com.accountabilityatlas.userservice.domain.UserSocialLinks;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialLinksRepository extends JpaRepository<UserSocialLinks, UUID> {}
