package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.domain.User;

public record AuthResult(User user, String accessToken, String refreshToken) {}
