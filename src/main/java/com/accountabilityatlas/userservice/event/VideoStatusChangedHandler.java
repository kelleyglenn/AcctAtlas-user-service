package com.accountabilityatlas.userservice.event;

import com.accountabilityatlas.userservice.service.UserStatsService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoStatusChangedHandler {

  private final UserStatsService userStatsService;

  @SqsListener("${app.sqs.user-video-status-events-queue:user-video-status-events}")
  public void handleVideoStatusChanged(VideoStatusChangedEvent event) {
    log.info(
        "Received VideoStatusChanged event for video {} ({} -> {})",
        event.videoId(),
        event.previousStatus(),
        event.newStatus());
    try {
      userStatsService.handleStatusChange(
          event.submittedBy(), event.previousStatus(), event.newStatus());
    } catch (Exception e) {
      log.error(
          "Failed to handle VideoStatusChanged event for video {}: {}",
          event.videoId(),
          e.getMessage(),
          e);
      throw e;
    }
  }
}
