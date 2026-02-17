package com.accountabilityatlas.userservice.client;

import java.util.Optional;

/** Client for fetching data from the YouTube Data API. */
public interface YouTubeClient {

  /**
   * Fetches the thumbnail URL for the given YouTube channel.
   *
   * @param channelId the YouTube channel ID (e.g. "UCtest123")
   * @return the thumbnail URL, or empty if unavailable
   */
  Optional<String> getChannelThumbnailUrl(String channelId);
}
