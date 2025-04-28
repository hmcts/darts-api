package uk.gov.hmcts.darts.transcriptions.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionRequest;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

@RequiredArgsConstructor
@Service
public class TranscriptionsApiImpl implements TranscriptionsApi {

    private final TranscriptionService transcriptionService;

    @Override
    public RequestTranscriptionResponse saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails, boolean isManual) {
        return transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, isManual);
    }

    @Override
    public UpdateTranscriptionResponse updateTranscription(Long transcriptionId, UpdateTranscriptionRequest updateTranscription) {
        return transcriptionService.updateTranscription(transcriptionId, updateTranscription, true);
    }
}
