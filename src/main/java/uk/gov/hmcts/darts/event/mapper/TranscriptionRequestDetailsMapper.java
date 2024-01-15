package uk.gov.hmcts.darts.event.mapper;

import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

@SuppressWarnings("HideUtilityClassConstructor")
public class TranscriptionRequestDetailsMapper {

    public static TranscriptionRequestDetails transcriptionRequestDetailsFrom(DartsEvent dartsEvent, HearingEntity hearingEntity) {
        var transcriptionRequestDetails = new TranscriptionRequestDetails();
        transcriptionRequestDetails.setCaseId(hearingEntity.getCourtCase().getId());
        transcriptionRequestDetails.setStartDateTime(dartsEvent.getStartTime());
        transcriptionRequestDetails.setEndDateTime(dartsEvent.getEndTime());
        transcriptionRequestDetails.setHearingId(hearingEntity.getId());

        return transcriptionRequestDetails;
    }
}
