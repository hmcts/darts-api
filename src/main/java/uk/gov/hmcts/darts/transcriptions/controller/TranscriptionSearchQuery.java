package uk.gov.hmcts.darts.transcriptions.controller;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.util.List;
import java.util.Set;

public interface TranscriptionSearchQuery {

    Set<TranscriptionSearchResult> searchTranscriptions(TranscriptionSearchRequest request, List<Integer> transcriptionIds);

    List<Integer> findTranscriptionsCurrentlyOwnedBy(String owner);

}
