package com.accountabilityatlas.userservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of {@link YouTubeClient} used when no YouTube API key is configured. Always
 * returns null.
 */
@Component
@ConditionalOnMissingBean(RestYouTubeClient.class)
@Slf4j
public class NoOpYouTubeClient implements YouTubeClient {

  public NoOpYouTubeClient() {
    log.info("YouTube API key not configured — YouTube avatar resolution disabled");
  }

  @Override
  @Nullable
  public String getChannelThumbnailUrl(String channelId) {
    return null;
  }
}
