package uk.gov.hmcts.darts.transcriptions.controller;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.util.List;

public interface TranscriptionSearchQuery {

    List<Integer> findTranscriptionsCurrentlyOwnedBy(String owner);

    List<TranscriptionSearchResult> searchTranscriptionsByFilters(
        TranscriptionSearchRequest request,
        List<Integer> transcriptionsForOwner);
}
