package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioIngestionConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.AudioIngestionEntity;
import uk.gov.hmcts.darts.audio.service.AudioIngestionService;
import uk.gov.hmcts.darts.common.enums.AudioIngestionTemporaryFailureMode;
import uk.gov.hmcts.darts.common.repository.AudioIngestionRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioIngestionServiceImpl implements AudioIngestionService {
    private final AudioIngestionRepository audioIngestionRepository;
    private final AudioIngestionConfigurationProperties config;

    @Override
    public void process() {
        String claimOwner = getClaimOwner();

        log.info(
            "Starting external audio ingestion job. claimOwner={}",
            claimOwner
        );

        OffsetDateTime cutoff =
            OffsetDateTime.now(ZoneOffset.UTC)
                .minus(config.getProcessingTimeout());

        List<Long> claimedIds =
            audioIngestionRepository.claimForProcessing(
                claimOwner,
                cutoff,
                config.getBatchSize()
            );

        if (claimedIds.isEmpty()) {
            log.info(
                "No external audio ingestion items available. claimOwner={}",
                claimOwner
            );
            return;
        }

        List<AudioIngestionEntity> claimedItems =
            audioIngestionRepository.findAllById(claimedIds);

        log.info(
            "External audio ingestion job claimed {} items. "
                + "claimOwner={}, ids={}",
            claimedItems.size(),
            claimOwner,
            claimedIds
        );

        processItems(claimedItems, claimOwner);
    }

    private void processItems(
        List<AudioIngestionEntity> claimedItems,
        String claimOwner
    ) {
        AudioIngestionTemporaryFailureMode failureMode =
            config.getTemporaryFailureMode();

        for (int index = 0; index < claimedItems.size(); index++) {
            AudioIngestionEntity item = claimedItems.get(index);

            log.info(
                "Would process external audio item. "
                    + "claimOwner={}, id={}, sourceFullFilename={}",
                claimOwner,
                item.getId(),
                item.getSourceFullFilename()
            );

            if (shouldTemporarilyFail(failureMode, index)) {
                log.warn(
                    "Simulating temporary failure for external audio item. "
                        + "claimOwner={}, id={}, sourceFullFilename={}",
                    claimOwner,
                    item.getId(),
                    item.getSourceFullFilename()
                );

                // Intentionally leave the item in PROCESSING.
                // It becomes eligible again after processingTimeout.
                continue;
            }

            audioIngestionRepository.deleteProcessed(
                item.getId().longValue(),
                claimOwner
            );

            log.info(
                "Successfully simulated processing external audio item. "
                    + "claimOwner={}, id={}",
                claimOwner,
                item.getId()
            );
        }
    }

    private boolean shouldTemporarilyFail(
        AudioIngestionTemporaryFailureMode failureMode,
        int index
    ) {
        return switch (failureMode) {
            case ALL -> true;
            case FIRST_TWO -> index < 2;
            case NONE -> false;
        };
    }

    private String getClaimOwner() {
        return System.getenv().getOrDefault(
            "HOSTNAME",
            UUID.randomUUID().toString()
        );
    }
}