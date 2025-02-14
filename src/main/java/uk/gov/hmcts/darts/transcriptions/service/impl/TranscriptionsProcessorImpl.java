package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionsProcessor;

import java.time.OffsetDateTime;
import java.util.List;

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

    @Override
    public void closeTranscriptions(Integer batchSize) {
        try {
            List<TranscriptionStatusEntity> finishedTranscriptionStatuses = transcriptionService.getFinishedTranscriptionStatuses();
            OffsetDateTime lastCreatedDateTime = currentTimeHelper.currentOffsetDateTime()
                .minus(transcriptionConfigurationProperties.getMaxCreatedByDuration());
            List<Integer> transcriptionsToBeClosed =
                transcriptionRepository.findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
                    finishedTranscriptionStatuses,
                    lastCreatedDateTime,
                    Limit.of(batchSize)
                );
            if (isNull(transcriptionsToBeClosed) || transcriptionsToBeClosed.isEmpty()) {
                log.debug("No transcriptions to be closed off");
            } else {
                log.info("Number of transcriptions to be closed off: {}", transcriptionsToBeClosed.size());
                for (Integer transcriptionToBeClosed : transcriptionsToBeClosed) {
                    transcriptionService.closeTranscription(transcriptionToBeClosed, AUTOMATICALLY_CLOSED_TRANSCRIPTION);
                }
            }
        } catch (Exception e) {
            log.error("Unable to close transcriptions {}", e.getMessage());
        }
    }
}
