package uk.gov.hmcts.darts.transcriptions.api;

import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;

public interface TranscriptionsApi {
    RequestTranscriptionResponse saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails, boolean isManual);

    UpdateTranscriptionResponse updateTranscription(Long transcriptionId, UpdateTranscriptionRequest updateTranscription);

}
