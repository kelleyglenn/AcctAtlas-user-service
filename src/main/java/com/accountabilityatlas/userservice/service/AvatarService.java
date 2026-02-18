package com.accountabilityatlas.userservice.service;

import com.accountabilityatlas.userservice.client.YouTubeClient;
import com.accountabilityatlas.userservice.domain.UserSocialLinks;
import com.accountabilityatlas.userservice.web.model.AvatarSources;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves avatar URLs from Gravatar (email hash) and YouTube (channel thumbnail). */
@Service
@RequiredArgsConstructor
public class AvatarService {

  private final YouTubeClient youTubeClient;

  /**
   * Generates a Gravatar URL from the user's email address.
   *
   * @param email the user's email address
   * @return Gravatar URL with {@code d=404} to return 404 when no avatar exists
   */
  public URI resolveGravatarUrl(String email) {
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    String hash = md5Hex(normalized);
    return URI.create("https://gravatar.com/avatar/" + hash + "?d=404");
  }

  /**
   * Fetches the YouTube channel thumbnail URL via the YouTube Data API.
   *
   * @param youtubeChannelId the YouTube channel ID
   * @return the thumbnail URL, or empty if unavailable
   */
  public Optional<URI> resolveYoutubeThumbnailUrl(String youtubeChannelId) {
    return Optional.ofNullable(youTubeClient.getChannelThumbnailUrl(youtubeChannelId))
        .map(URI::create);
  }

  /**
   * Builds {@link AvatarSources} for a user. Gravatar is always resolved. YouTube is resolved only
   * when the user has a YouTube channel ID in their social links.
   *
   * @param email the user's email address
   * @param socialLinks the user's social links, may be null
   * @return populated avatar sources
   */
  public AvatarSources getAvatarSources(String email, UserSocialLinks socialLinks) {
    AvatarSources sources = new AvatarSources();
    sources.setGravatar(resolveGravatarUrl(email));

    if (socialLinks != null
        && socialLinks.getYoutube() != null
        && !socialLinks.getYoutube().isBlank()) {
      resolveYoutubeThumbnailUrl(socialLinks.getYoutube()).ifPresent(sources::setYoutube);
    }

    return sources;
  }

  private String md5Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 algorithm not available", e);
    }
  }
}
