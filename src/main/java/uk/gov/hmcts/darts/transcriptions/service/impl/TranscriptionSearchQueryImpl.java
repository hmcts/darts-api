package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionIdsAndLatestWorkflowTs;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchRequest;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionSearchQuery;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TranscriptionSearchQueryImpl implements TranscriptionSearchQuery {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;

    @Override
    public List<TranscriptionSearchResult> searchTranscriptions(TranscriptionSearchRequest request, List<Long> transcriptionIds) {

        Optional<TranscriptionStatusEntity> transcriptionStatusEntity = transcriptionStatusRepository.findById(TranscriptionStatusEnum.APPROVED.getId());
        return transcriptionRepository.searchTranscriptionsFilteringOn(
            transcriptionIds,
            request.getCaseNumber(),
            request.getCourthouseDisplayName(),
            request.getHearingDate(),
            getCreatedFromTs(request),
            getCreatedTo(request),
            request.getIsManualTranscription(),
            request.getRequestedBy(),
            transcriptionStatusEntity.orElse(null)
        );
    }

    @Override
    public List<Long> findTranscriptionsIdsCurrentlyOwnedBy(String owner) {
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
