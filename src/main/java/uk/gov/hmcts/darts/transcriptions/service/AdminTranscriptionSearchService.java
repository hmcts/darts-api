package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentRequest;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface AdminTranscriptionSearchService {

    List<TranscriptionSearchResponse> searchTranscriptions(TranscriptionSearchRequest request);


    List<SearchTranscriptionDocumentResponse> searchTranscriptions(SearchTranscriptionDocumentRequest request);

    List<GetTranscriptionDetailAdminResponse> getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom);
}