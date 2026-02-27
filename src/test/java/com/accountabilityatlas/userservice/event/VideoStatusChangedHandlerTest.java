package com.accountabilityatlas.userservice.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.accountabilityatlas.userservice.service.UserStatsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoStatusChangedHandlerTest {

  @Mock private UserStatsService userStatsService;
  @InjectMocks private VideoStatusChangedHandler videoStatusChangedHandler;

  @Test
  void handleVideoStatusChanged_delegatesToUserStatsService() {
    UUID submittedBy = UUID.randomUUID();
    VideoStatusChangedEvent event =
        new VideoStatusChangedEvent(
            UUID.randomUUID(),
            submittedBy,
            List.of(UUID.randomUUID()),
            "PENDING",
            "APPROVED",
            Instant.now());

    videoStatusChangedHandler.handleVideoStatusChanged(event);

    verify(userStatsService).handleStatusChange(submittedBy, "PENDING", "APPROVED");
  }

  @Test
  void handleVideoStatusChanged_serviceFailure_rethrowsException() {
    UUID submittedBy = UUID.randomUUID();
    VideoStatusChangedEvent event =
        new VideoStatusChangedEvent(
            UUID.randomUUID(), submittedBy, List.of(), "APPROVED", "REJECTED", Instant.now());

    RuntimeException exception = new RuntimeException("DB error");
    doThrow(exception)
        .when(userStatsService)
        .handleStatusChange(submittedBy, "APPROVED", "REJECTED");

    assertThatThrownBy(() -> videoStatusChangedHandler.handleVideoStatusChanged(event))
        .isSameAs(exception);
  }
}
