package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

public interface TranscriptionService {
    void saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails);
}
