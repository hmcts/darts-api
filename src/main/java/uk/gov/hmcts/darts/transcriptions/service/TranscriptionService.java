package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.*;

import java.util.List;

public interface TranscriptionService {

    RequestTranscriptionResponse saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails);

    UpdateTranscriptionResponse updateTranscription(Integer transcriptionId, UpdateTranscription updateTranscription);

    void closeTranscriptions();

    void closeTranscription(Integer transcriptionId, String transcriptionComment);

    List<TranscriptionTypeResponse> getTranscriptionTypes();

    List<TranscriptionUrgencyResponse> getTranscriptionUrgencies();

    TranscriptionResponse getTranscription(Integer transcriptionId);
}
