package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface AdminTranscriptionSearchService {

    List<TranscriptionSearchResponse> searchTranscriptions(TranscriptionSearchRequest request);

    List<GetTranscriptionDetailResponse> getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom);
}