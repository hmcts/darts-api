package uk.gov.hmcts.darts.commonplatform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.darts.commonplatform.model.CommonPlatformEventRequest;
import uk.gov.hmcts.darts.commonplatform.model.CommonPlatformSubmissionResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CommonPlatformControllerTest {

    private final CommonPlatformController controller = new CommonPlatformController();

    @Test
    void givenDailyListRequestWithMessageAndCorrelationId_whenSubmittingDailyList_thenReturnsNotImplemented() {
        ResponseEntity<CommonPlatformSubmissionResponse> response = controller.submitCommonPlatformDailyList(
            "message-123",
            "<DailyList/>",
            "correlation-456"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void givenDailyListRequestWithoutCorrelationId_whenSubmittingDailyList_thenReturnsNotImplemented() {
        ResponseEntity<CommonPlatformSubmissionResponse> response = controller.submitCommonPlatformDailyList(
            "message-123",
            "<DailyList/>",
            null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void givenDailyListRequestWithBlankMessageId_whenSubmittingDailyList_thenReturnsNotImplemented() {
        ResponseEntity<CommonPlatformSubmissionResponse> response = controller.submitCommonPlatformDailyList(
            "",
            "<DailyList/>",
            "correlation-456"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void givenEventRequestWithHeaders_whenSubmittingEvent_thenReturnsNotImplemented() {
        CommonPlatformEventRequest request = new CommonPlatformEventRequest();

        ResponseEntity<CommonPlatformSubmissionResponse> response = controller.submitCommonPlatformEvent(
            "message-123",
            request,
            "correlation-456"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void givenEventRequestWithoutCorrelationId_whenSubmittingEvent_thenReturnsNotImplemented() {
        CommonPlatformEventRequest request = new CommonPlatformEventRequest();

        ResponseEntity<CommonPlatformSubmissionResponse> response = controller.submitCommonPlatformEvent(
            "message-123",
            request,
            null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void givenNullEventRequest_whenSubmittingEvent_thenReturnsNotImplemented() {
        ResponseEntity<CommonPlatformSubmissionResponse> response = controller.submitCommonPlatformEvent(
            "message-123",
            null,
            "correlation-456"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNull();
    }
}

