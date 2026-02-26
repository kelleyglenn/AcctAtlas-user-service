package com.accountabilityatlas.userservice.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.accountabilityatlas.userservice.service.UserStatsService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoSubmittedHandlerTest {

  @Mock private UserStatsService userStatsService;
  @InjectMocks private VideoSubmittedHandler videoSubmittedHandler;

  @Test
  void handleVideoSubmitted_delegatesToUserStatsService() {
    UUID submitterId = UUID.randomUUID();
    VideoSubmittedEvent event =
        new VideoSubmittedEvent(
            UUID.randomUUID(),
            submitterId,
            "NEW",
            "Test Video",
            Set.of("FIRST"),
            List.of(UUID.randomUUID()),
            Instant.now());

    videoSubmittedHandler.handleVideoSubmitted(event);

    verify(userStatsService).incrementSubmissionCount(submitterId);
  }

  @Test
  void handleVideoSubmitted_serviceFailure_rethrowsException() {
    UUID submitterId = UUID.randomUUID();
    VideoSubmittedEvent event =
        new VideoSubmittedEvent(
            UUID.randomUUID(), submitterId, "TRUSTED", "Test", Set.of(), List.of(), Instant.now());

    RuntimeException exception = new RuntimeException("DB error");
    doThrow(exception).when(userStatsService).incrementSubmissionCount(submitterId);

    assertThatThrownBy(() -> videoSubmittedHandler.handleVideoSubmitted(event)).isSameAs(exception);
  }
}
