package uk.gov.hmcts.darts.event.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.event.mapper.TranscriptionRequestDetailsMapper.transcriptionRequestDetailsFrom;

class TranscriptionRequestDetailsMapperTest {

    private static DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
              .messageId("some-message-id")
              .type("40790")
              .eventId("1")
              .courthouse("some-court-house")
              .courtroom("some-court-room")
              .eventText("some-text");
    }

    @Test
    void createsTranscriptionRequestWithCorrectDetails() {
        var dartsEvent = someMinimalDartsEvent();
        var hearingEntity = someMinimalHearing();

        var transcriptionRequestDetails = transcriptionRequestDetailsFrom(dartsEvent, hearingEntity);

        assertThat(transcriptionRequestDetails.getCaseId()).isEqualTo(1);
        assertThat(transcriptionRequestDetails.getHearingId()).isEqualTo(2);
        assertThat(transcriptionRequestDetails.getStartDateTime()).isEqualTo(dartsEvent.getStartTime());
        assertThat(transcriptionRequestDetails.getEndDateTime()).isEqualTo(dartsEvent.getEndTime());
    }

    private HearingEntity someMinimalHearing() {
        var courtCase = new CourtCaseEntity();
        courtCase.setId(1);

        var hearingEntity = new HearingEntity();
        hearingEntity.setId(2);
        hearingEntity.setCourtCase(courtCase);

        return hearingEntity;
    }
}
