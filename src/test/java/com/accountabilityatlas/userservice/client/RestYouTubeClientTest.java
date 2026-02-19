package com.accountabilityatlas.userservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestYouTubeClientTest {

  private static final String API_KEY = "test-api-key";
  private static final String CHANNEL_ID = "UCtest123";
  private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

  private MockRestServiceServer server;
  private RestYouTubeClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RestYouTubeClient(API_KEY, builder.build());
  }

  @Test
  void getChannelThumbnailUrl_returnsThumbnailUrl_whenChannelFound() {
    // Arrange
    String expectedUrl = "https://yt3.ggpht.com/some-thumbnail.jpg";
    server
        .expect(requestTo(channelRequestUrl()))
        .andRespond(
            withSuccess(
                """
                {
                  "items": [{
                    "snippet": {
                      "thumbnails": {
                        "default": {
                          "url": "%s"
                        }
                      }
                    }
                  }]
                }
                """
                    .formatted(expectedUrl),
                MediaType.APPLICATION_JSON));

    // Act
    String result = client.getChannelThumbnailUrl(CHANNEL_ID);

    // Assert
    assertThat(result).isEqualTo(expectedUrl);
    server.verify();
  }

  @Test
  void getChannelThumbnailUrl_returnsNull_whenItemsArrayEmpty() {
    // Arrange
    server
        .expect(requestTo(channelRequestUrl()))
        .andRespond(
            withSuccess(
                """
                {"items": []}
                """,
                MediaType.APPLICATION_JSON));

    // Act
    String result = client.getChannelThumbnailUrl(CHANNEL_ID);

    // Assert
    assertThat(result).isNull();
    server.verify();
  }

  @Test
  void getChannelThumbnailUrl_returnsNull_whenItemsMissing() {
    // Arrange
    server
        .expect(requestTo(channelRequestUrl()))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    // Act
    String result = client.getChannelThumbnailUrl(CHANNEL_ID);

    // Assert
    assertThat(result).isNull();
    server.verify();
  }

  @Test
  void getChannelThumbnailUrl_returnsNull_whenThumbnailNodeMissing() {
    // Arrange
    server
        .expect(requestTo(channelRequestUrl()))
        .andRespond(
            withSuccess(
                """
                {
                  "items": [{
                    "snippet": {
                      "thumbnails": {}
                    }
                  }]
                }
                """,
                MediaType.APPLICATION_JSON));

    // Act
    String result = client.getChannelThumbnailUrl(CHANNEL_ID);

    // Assert
    assertThat(result).isNull();
    server.verify();
  }

  @Test
  void getChannelThumbnailUrl_returnsNull_whenApiThrowsException() {
    // Arrange
    server
        .expect(requestTo(channelRequestUrl()))
        .andRespond(withException(new IOException("Connection refused")));

    // Act
    String result = client.getChannelThumbnailUrl(CHANNEL_ID);

    // Assert
    assertThat(result).isNull();
    server.verify();
  }

  private String channelRequestUrl() {
    return BASE_URL + "/channels?part=snippet&id=" + CHANNEL_ID + "&key=" + API_KEY;
  }
}
