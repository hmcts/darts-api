package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsProcessor;

import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.AMEND_TRANSCRIPTION_WORKFLOW;
import static java.util.Objects.isNull;

@RequiredArgsConstructor
@Service
@Slf4j
public class TranscriptionsProcessorImpl implements TranscriptionsProcessor {

    private static final String AUTOMATICALLY_CLOSED_TRANSCRIPTION = "Automatically closed transcription";

    private final TranscriptionConfigurationProperties transcriptionConfigurationProperties;
    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionService transcriptionService;
    private final CurrentTimeHelper currentTimeHelper;
    private final AuditApi auditApi;

    @Override
    public void closeTranscriptions(Integer batchSize) {
        try {
            List<TranscriptionStatusEntity> finishedTranscriptionStatuses = transcriptionService.getFinishedTranscriptionStatuses();
            OffsetDateTime lastCreatedDateTime = currentTimeHelper.currentOffsetDateTime()
                .minus(transcriptionConfigurationProperties.getMaxCreatedByDuration());
            List<Long> transcriptionsToBeClosed =
                transcriptionRepository.findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
                    finishedTranscriptionStatuses,
                    lastCreatedDateTime,
                    Limit.of(batchSize)
                );
            if (isNull(transcriptionsToBeClosed) || transcriptionsToBeClosed.isEmpty()) {
                log.debug("No transcriptions to be closed off");
            } else {
                log.info("Number of transcriptions to be closed off: {} out of a batch size {}", transcriptionsToBeClosed.size(), batchSize);
                boolean transcriptionClosed = false;
                for (Long transcriptionToBeClosed : transcriptionsToBeClosed) {
                    if (transcriptionService.closeTranscription(transcriptionToBeClosed, AUTOMATICALLY_CLOSED_TRANSCRIPTION)) {
                        transcriptionClosed = true;
                    }
                }

                if (transcriptionClosed) {
                    auditApi.record(AMEND_TRANSCRIPTION_WORKFLOW);
                }
            }
        } catch (Exception e) {
            log.error("Unable to close transcriptions {}", e.getMessage());
        }
    }
}
