package com.accountabilityatlas.userservice.event;

import com.accountabilityatlas.userservice.service.UserStatsService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoSubmittedHandler {

  private final UserStatsService userStatsService;

  @SqsListener("${app.sqs.user-video-events-queue:user-video-events}")
  public void handleVideoSubmitted(VideoSubmittedEvent event) {
    log.info(
        "Received VideoSubmitted event for video {} from submitter {}",
        event.videoId(),
        event.submitterId());
    try {
      userStatsService.incrementSubmissionCount(event.submitterId());
    } catch (Exception e) {
      log.error(
          "Failed to handle VideoSubmitted event for video {}: {}",
          event.videoId(),
          e.getMessage(),
          e);
      throw e;
    }
  }
}
