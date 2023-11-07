package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class TranscriptionRequestHandler extends EventHandlerBase {

    private final TranscriptionsApi transcriptionsApi;

    @Override
    @Transactional
    public void handle(final DartsEvent dartsEvent) {
        //save the event in the database
        dartsEvent.setDateTime(dartsEvent.getStartTime());
        var createdHearing = createHearing(dartsEvent);

        //create automatic transcription request
        TranscriptionRequestDetails transcriptionRequestDetails = new TranscriptionRequestDetails();
        transcriptionRequestDetails.setCaseId(createdHearing.getHearingEntity().getCourtCase().getId());
        transcriptionRequestDetails.setTranscriptionTypeId(TranscriptionTypeEnum.OTHER.getId());
        transcriptionRequestDetails.setUrgencyId(TranscriptionUrgencyEnum.OVERNIGHT.getId());
        transcriptionRequestDetails.setStartDateTime(dartsEvent.getStartTime());
        transcriptionRequestDetails.setEndDateTime(dartsEvent.getEndTime());
        transcriptionRequestDetails.setHearingId(createdHearing.getHearingEntity().getId());
        RequestTranscriptionResponse transcriptionResponse = transcriptionsApi.saveTranscriptionRequest(
            transcriptionRequestDetails, false);

        //automatically approve the transcription request
        UpdateTranscription updateTranscription = new UpdateTranscription();
        updateTranscription.setTranscriptionStatusId(TranscriptionStatusEnum.APPROVED.getId());
        updateTranscription.setWorkflowComment("Transcription Automatically approved");
        transcriptionsApi.updateTranscription(transcriptionResponse.getTranscriptionId(), updateTranscription);
    }

}
