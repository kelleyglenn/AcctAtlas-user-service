package com.accountabilityatlas.userservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class CacheConfigTest {

  private final CacheConfig cacheConfig = new CacheConfig();

  @Test
  void cacheManager_createsManagerSuccessfully() {
    RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);

    RedisCacheManager manager = cacheConfig.cacheManager(mockFactory);

    assertThat(manager).isNotNull();
  }
}
