package uk.gov.hmcts.darts.transcriptions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.repository.TranscriptionIdsAndLatestWorkflowTs;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.*;
import static org.springframework.util.CollectionUtils.*;

@Service
@RequiredArgsConstructor
public class TranscriptionSearchQueryImpl implements TranscriptionSearchQuery {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;

    @Override
    public List<TranscriptionSearchResult> searchNonLegacyTranscriptions(TranscriptionSearchRequest request) {
        List<Integer> transcriptionsForOwner = new ArrayList<>();
        if (request.getOwner() != null) {
            transcriptionsForOwner = findTranscriptionsCurrentlyOwnedBy(request.getOwner());
            if (transcriptionsForOwner.isEmpty()) {
                return emptyList();
            }
            if (transcriptionIdFilterIsNotContainedInTheTranscriptionsOwnedBy(request, transcriptionsForOwner)) {
                return emptyList();
            }
        }

        var transcriptionIds = new ArrayList<Integer>();
        if (request.getTranscriptionId() != null) {
            transcriptionIds.add(request.getTranscriptionId());
        } else if (!isEmpty(transcriptionsForOwner)) {
            transcriptionIds.addAll(transcriptionsForOwner);
        } else {
            transcriptionIds = null;
        }

        return transcriptionRepository.searchNonLegacyTranscriptionsFilteringOn(
            transcriptionIds,
            request.getCaseNumber(),
            request.getCourthouseDisplayName(),
            request.getHearingDate(),
            getCreatedFromTs(request),
            getCreatedTo(request),
            request.getIsManualTranscription(),
            request.getRequestedBy()
        );
    }

    @Override
    public List<TranscriptionSearchResult> searchLegacyTranscriptions(TranscriptionSearchRequest request) {
        List<Integer> transcriptionsForOwner = new ArrayList<>();
        if (request.getOwner() != null) {
            transcriptionsForOwner = findTranscriptionsCurrentlyOwnedBy(request.getOwner());
            if (transcriptionsForOwner.isEmpty()) {
                return emptyList();
            }
            if (transcriptionIdFilterIsNotContainedInTheTranscriptionsOwnedBy(request, transcriptionsForOwner)) {
                return emptyList();
            }
        }

        var transcriptionIds = new ArrayList<Integer>();
        if (request.getTranscriptionId() != null) {
            transcriptionIds.add(request.getTranscriptionId());
        } else if (!isEmpty(transcriptionsForOwner)) {
            transcriptionIds.addAll(transcriptionsForOwner);
        } else {
            transcriptionIds = null;
        }

        return transcriptionRepository.searchLegacyTranscriptionsFilteringOn(
            transcriptionIds,
            request.getCaseNumber(),
            request.getCourthouseDisplayName(),
            request.getHearingDate(),
            getCreatedFromTs(request),
            getCreatedTo(request),
            request.getIsManualTranscription(),
            request.getRequestedBy()
        );
    }

    private static OffsetDateTime getCreatedTo(TranscriptionSearchRequest request) {
        return request.getRequestedAtTo() == null ? null : OffsetDateTime.of(request.getRequestedAtTo(), LocalTime.MAX, ZoneOffset.UTC);
    }

    private static OffsetDateTime getCreatedFromTs(TranscriptionSearchRequest request) {
        return request.getRequestedAtFrom() == null ? null : OffsetDateTime.of(request.getRequestedAtFrom(), LocalTime.MIN, ZoneOffset.UTC);
    }

    private static boolean transcriptionIdFilterIsNotContainedInTheTranscriptionsOwnedBy(TranscriptionSearchRequest request, List<Integer> transcriptionsForOwner) {
        return request.getTranscriptionId() != null
            && !isEmpty(transcriptionsForOwner)
            && !transcriptionsForOwner.contains(request.getTranscriptionId());
    }

    private List<Integer> findTranscriptionsCurrentlyOwnedBy(String owner) {
        return transcriptionWorkflowRepository.findWorkflowOwnedBy(owner).stream()
            .map(TranscriptionIdsAndLatestWorkflowTs::transcriptionId).toList();
    }

}
