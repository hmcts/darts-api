package uk.gov.hmcts.darts.transcriptions.controller;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResponse;

import java.util.List;

public interface AdminTranscriptionSearchService {

    List<TranscriptionSearchResponse> searchTranscriptions(TranscriptionSearchRequest request);
}
