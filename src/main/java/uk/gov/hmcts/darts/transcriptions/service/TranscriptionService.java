package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;

public interface TranscriptionService {

    void saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails);

    UpdateTranscriptionResponse updateTranscription(Integer transcriptionId, UpdateTranscription updateTranscription);

}
