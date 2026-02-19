package com.accountabilityatlas.userservice.client;

import org.springframework.lang.Nullable;

/** Client for fetching data from the YouTube Data API. */
public interface YouTubeClient {

  /**
   * Fetches the thumbnail URL for the given YouTube channel.
   *
   * @param channelId the YouTube channel ID (e.g. "UCtest123")
   * @return the thumbnail URL, or null if unavailable
   */
  @Nullable
  String getChannelThumbnailUrl(String channelId);
}
