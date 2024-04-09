package uk.gov.hmcts.darts.transcriptions.controller;

import jakarta.transaction.Transactional;
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
    public List<TranscriptionSearchResult> searchTranscriptionsByFilters(TranscriptionSearchRequest request, List<Integer> transcriptionsForOwner) {
        if (request.getTranscriptionId() != null
            && !isEmpty(transcriptionsForOwner)
            && !transcriptionsForOwner.contains(request.getTranscriptionId()))
        {
            return emptyList();
        }

        var transcriptionIds = new ArrayList<Integer>();
        if (request.getTranscriptionId() != null) {
            transcriptionIds.add(request.getTranscriptionId());
        } else if (!isEmpty(transcriptionsForOwner)) {
            transcriptionIds.addAll(transcriptionsForOwner);
        } else {
            transcriptionIds = null;
        }

        var createdFrom = request.getRequestedAtFrom() == null ? null : OffsetDateTime.of(request.getRequestedAtFrom(), LocalTime.MIN, ZoneOffset.UTC);
        var createdTo = request.getRequestedAtTo() == null ? null : OffsetDateTime.of(request.getRequestedAtTo(), LocalTime.MAX, ZoneOffset.UTC);
        var hearingDate = request.getHearingDate() == null ? null : request.getHearingDate();

        return transcriptionRepository.searchFilteringOn(
            transcriptionIds,
            request.getCaseNumber(),
            request.getCourthouseDisplayName(),
            hearingDate,
            createdFrom,
            createdTo,
            request.getIsManualTranscription(),
            request.getRequestedBy()
        );
    }

    @Override
    @Transactional
    public List<Integer> findTranscriptionsCurrentlyOwnedBy(String owner) {
        return transcriptionWorkflowRepository.findWorkflowOwnedBy(owner).stream()
            .map(TranscriptionIdsAndLatestWorkflowTs::transcriptionId).toList();
    }

}
