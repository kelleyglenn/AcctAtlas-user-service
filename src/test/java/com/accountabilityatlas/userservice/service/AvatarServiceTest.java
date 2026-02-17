package com.accountabilityatlas.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.userservice.client.YouTubeClient;
import com.accountabilityatlas.userservice.domain.UserSocialLinks;
import com.accountabilityatlas.userservice.web.model.AvatarSources;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

  private static final String TEST_EMAIL = "test@example.com";

  // MD5 hash of "test@example.com"
  private static final String TEST_EMAIL_MD5 = "55502f40dc8b7c769880b10874abc9d0";

  @Mock private YouTubeClient youTubeClient;

  private AvatarService avatarService;

  @BeforeEach
  void setUp() {
    avatarService = new AvatarService(youTubeClient);
  }

  @Test
  void resolveGravatarUrl_returnsUrlWithMd5Hash() {
    URI result = avatarService.resolveGravatarUrl(TEST_EMAIL);

    assertThat(result)
        .isEqualTo(URI.create("https://gravatar.com/avatar/" + TEST_EMAIL_MD5 + "?d=404"));
  }

  @Test
  void resolveGravatarUrl_normalizesMixedCaseAndWhitespace() {
    URI result = avatarService.resolveGravatarUrl("  Test@Example.COM  ");

    // After trim + lowercase, "test@example.com" should produce the same hash
    assertThat(result)
        .isEqualTo(URI.create("https://gravatar.com/avatar/" + TEST_EMAIL_MD5 + "?d=404"));
  }

  @Test
  void getAvatarSources_returnsGravatarAlways_evenWithNoSocialLinks() {
    AvatarSources result = avatarService.getAvatarSources(TEST_EMAIL, null);

    assertThat(result.getGravatar())
        .isEqualTo(URI.create("https://gravatar.com/avatar/" + TEST_EMAIL_MD5 + "?d=404"));
    assertThat(result.getYoutube()).isNull();
  }

  @Test
  void getAvatarSources_returnsGravatarAndYoutube_whenYoutubeChannelPresent() {
    String channelId = "UCtest123";
    String thumbnailUrl = "https://yt3.ggpht.com/some-thumbnail.jpg";

    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(UUID.randomUUID());
    socialLinks.setYoutube(channelId);

    when(youTubeClient.getChannelThumbnailUrl(channelId)).thenReturn(Optional.of(thumbnailUrl));

    AvatarSources result = avatarService.getAvatarSources(TEST_EMAIL, socialLinks);

    assertThat(result.getGravatar())
        .isEqualTo(URI.create("https://gravatar.com/avatar/" + TEST_EMAIL_MD5 + "?d=404"));
    assertThat(result.getYoutube()).isEqualTo(URI.create(thumbnailUrl));
  }

  @Test
  void getAvatarSources_returnsNullYoutube_whenSocialLinksHaveNoYoutube() {
    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(UUID.randomUUID());
    // youtube is null

    AvatarSources result = avatarService.getAvatarSources(TEST_EMAIL, socialLinks);

    assertThat(result.getGravatar()).isNotNull();
    assertThat(result.getYoutube()).isNull();
  }

  @Test
  void getAvatarSources_returnsNullYoutube_whenYoutubeIsBlank() {
    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(UUID.randomUUID());
    socialLinks.setYoutube("   ");

    AvatarSources result = avatarService.getAvatarSources(TEST_EMAIL, socialLinks);

    assertThat(result.getGravatar()).isNotNull();
    assertThat(result.getYoutube()).isNull();
  }

  @Test
  void getAvatarSources_returnsNullYoutube_whenYoutubeClientReturnsEmpty() {
    String channelId = "UCnonexistent";

    UserSocialLinks socialLinks = new UserSocialLinks();
    socialLinks.setUserId(UUID.randomUUID());
    socialLinks.setYoutube(channelId);

    when(youTubeClient.getChannelThumbnailUrl(channelId)).thenReturn(Optional.empty());

    AvatarSources result = avatarService.getAvatarSources(TEST_EMAIL, socialLinks);

    assertThat(result.getGravatar()).isNotNull();
    assertThat(result.getYoutube()).isNull();
  }

  @Test
  void resolveYoutubeThumbnailUrl_returnsUri_whenClientReturnsThumbnail() {
    String channelId = "UCtest123";
    String thumbnailUrl = "https://yt3.ggpht.com/thumbnail.jpg";
    when(youTubeClient.getChannelThumbnailUrl(channelId)).thenReturn(Optional.of(thumbnailUrl));

    Optional<URI> result = avatarService.resolveYoutubeThumbnailUrl(channelId);

    assertThat(result).contains(URI.create(thumbnailUrl));
  }

  @Test
  void resolveYoutubeThumbnailUrl_returnsEmpty_whenClientReturnsEmpty() {
    String channelId = "UCnonexistent";
    when(youTubeClient.getChannelThumbnailUrl(channelId)).thenReturn(Optional.empty());

    Optional<URI> result = avatarService.resolveYoutubeThumbnailUrl(channelId);

    assertThat(result).isEmpty();
  }
}
