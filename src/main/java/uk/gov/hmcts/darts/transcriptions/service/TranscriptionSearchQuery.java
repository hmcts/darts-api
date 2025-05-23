package uk.gov.hmcts.darts.transcriptions.service;

import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;

import java.util.List;

public interface TranscriptionSearchQuery {

    List<TranscriptionSearchResult> searchTranscriptions(TranscriptionSearchRequest request, List<Long> transcriptionIds);

    List<Long> findTranscriptionsIdsCurrentlyOwnedBy(String owner);

}
