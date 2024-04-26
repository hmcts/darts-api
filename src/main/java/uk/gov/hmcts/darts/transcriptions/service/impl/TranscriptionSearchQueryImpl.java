package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionIdsAndLatestWorkflowTs;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionSearchQuery;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TranscriptionSearchQueryImpl implements TranscriptionSearchQuery {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;

    @Override
    public Set<TranscriptionSearchResult> searchTranscriptions(TranscriptionSearchRequest request, List<Integer> transcriptionIds) {

        var nonLegacyTranscriptions = transcriptionRepository.searchModernisedTranscriptionsFilteringOn(
            transcriptionIds,
            request.getCaseNumber(),
            request.getCourthouseDisplayName(),
            request.getHearingDate(),
            getCreatedFromTs(request),
            getCreatedTo(request),
            request.getIsManualTranscription(),
            request.getRequestedBy()
        );

        var legacyTranscriptions = transcriptionRepository.searchMigratedTranscriptionsFilteringOn(
            transcriptionIds,
            request.getCaseNumber(),
            request.getCourthouseDisplayName(),
            request.getHearingDate(),
            getCreatedFromTs(request),
            getCreatedTo(request),
            request.getIsManualTranscription(),
            request.getRequestedBy()
        );

        var combinedSearchResults = new HashSet<TranscriptionSearchResult>();
        combinedSearchResults.addAll(nonLegacyTranscriptions);
        combinedSearchResults.addAll(legacyTranscriptions);

        return combinedSearchResults;
    }

    @Override
    public List<Integer> findTranscriptionsCurrentlyOwnedBy(String owner) {
        return transcriptionWorkflowRepository.findWorkflowOwnedBy(owner).stream()
            .map(TranscriptionIdsAndLatestWorkflowTs::transcriptionId).toList();
    }

    private static OffsetDateTime getCreatedTo(TranscriptionSearchRequest request) {
        return request.getRequestedAtTo() == null ? null : OffsetDateTime.of(request.getRequestedAtTo(), LocalTime.MAX, ZoneOffset.UTC);
    }

    private static OffsetDateTime getCreatedFromTs(TranscriptionSearchRequest request) {
        return request.getRequestedAtFrom() == null ? null : OffsetDateTime.of(request.getRequestedAtFrom(), LocalTime.MIN, ZoneOffset.UTC);
    }

}
