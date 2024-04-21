package uk.gov.hmcts.darts.transcriptions.controller;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.util.List;

public interface TranscriptionSearchQuery {

    List<TranscriptionSearchResult> searchLegacyTranscriptions(TranscriptionSearchRequest request);

    List<TranscriptionSearchResult> searchNonLegacyTranscriptions(TranscriptionSearchRequest request);
}
