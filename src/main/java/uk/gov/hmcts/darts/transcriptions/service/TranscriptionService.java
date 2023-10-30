package uk.gov.hmcts.darts.transcriptions.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.transcriptions.model.AttachTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;

import java.util.List;

public interface TranscriptionService {

    RequestTranscriptionResponse saveTranscriptionRequest(TranscriptionRequestDetails transcriptionRequestDetails);

    UpdateTranscriptionResponse updateTranscription(Integer transcriptionId, UpdateTranscription updateTranscription);

    void closeTranscriptions();

    List<TranscriptionTypeResponse> getTranscriptionTypes();

    List<TranscriptionUrgencyResponse> getTranscriptionUrgencies();

    AttachTranscriptResponse attachTranscript(Integer transcriptionId, MultipartFile transcript);

}
