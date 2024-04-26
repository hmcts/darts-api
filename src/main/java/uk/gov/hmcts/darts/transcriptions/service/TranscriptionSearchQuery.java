package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;

import java.util.List;
import java.util.Set;

public interface TranscriptionSearchQuery {

    Set<TranscriptionSearchResult> searchTranscriptions(TranscriptionSearchRequest request, List<Integer> transcriptionIds);

    List<Integer> findTranscriptionsCurrentlyOwnedBy(String owner);

}
