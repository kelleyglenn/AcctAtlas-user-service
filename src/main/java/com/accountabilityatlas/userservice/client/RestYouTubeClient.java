package com.accountabilityatlas.userservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * YouTube Data API v3 client that fetches channel thumbnail URLs. Only active when {@code
 * app.youtube.api-key} is configured.
 */
@Component
@ConditionalOnProperty(name = "app.youtube.api-key")
@Slf4j
public class RestYouTubeClient implements YouTubeClient {

  private final RestClient restClient;
  private final String apiKey;

  public RestYouTubeClient(@Value("${app.youtube.api-key}") String apiKey) {
    this.apiKey = apiKey;
    this.restClient = RestClient.builder().baseUrl("https://www.googleapis.com/youtube/v3").build();
  }

  RestYouTubeClient(String apiKey, RestClient restClient) {
    this.apiKey = apiKey;
    this.restClient = restClient;
  }

  @Override
  @Cacheable(value = "youtube-thumbnails", key = "#channelId")
  @Nullable
  public String getChannelThumbnailUrl(String channelId) {
    try {
      JsonNode response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/channels")
                          .queryParam("part", "snippet")
                          .queryParam("id", channelId)
                          .queryParam("key", apiKey)
                          .build())
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(JsonNode.class);

      if (response == null) {
        return null;
      }

      JsonNode items = response.path("items");
      if (items.isMissingNode() || items.isEmpty()) {
        log.debug("No YouTube channel found for ID: {}", channelId);
        return null;
      }

      JsonNode thumbnailUrl =
          items.get(0).path("snippet").path("thumbnails").path("default").path("url");

      if (thumbnailUrl.isMissingNode() || thumbnailUrl.isNull()) {
        return null;
      }

      return thumbnailUrl.asText();
    } catch (Exception e) {
      log.warn("Failed to fetch YouTube thumbnail for channel {}: {}", channelId, e.getMessage());
      return null;
    }
  }
}
