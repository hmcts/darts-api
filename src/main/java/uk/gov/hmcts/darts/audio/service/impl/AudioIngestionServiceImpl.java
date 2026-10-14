package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioIngestionConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.AudioIngestionEntity;
import uk.gov.hmcts.darts.audio.service.AudioIngestionService;
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

        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC)
            .minus(config.getProcessingTimeout());

        List<Long> claimedIds = audioIngestionRepository.claimForProcessing(
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

        int temporaryFailureCount =
            audioIngestionRepository.getTemporaryFailureCount();

        List<AudioIngestionEntity> claimedItems =
            audioIngestionRepository.findAllById(claimedIds);

        log.info(
            "External audio ingestion job claimed {} items. "
                + "claimOwner={}, ids={}, temporaryFailureCount={}",
            claimedIds.size(),
            claimOwner,
            claimedIds,
            temporaryFailureCount
        );

        processItems(
            claimedItems,
            claimOwner,
            temporaryFailureCount
        );
    }

    private void processItems(
        List<AudioIngestionEntity> claimedItems,
        String claimOwner,
        int temporaryFailureCount
    ) {
        int failuresRemaining = temporaryFailureCount;

        for (AudioIngestionEntity item : claimedItems) {
            log.info(
                "Processing external audio item. "
                    + "claimOwner={}, id={}, sourceFullFilename={}",
                claimOwner,
                item.getId(),
                item.getSourceFullFilename()
            );

            if (failuresRemaining > 0) {
                log.warn(
                    "Simulating temporary failure for external audio item. "
                        + "claimOwner={}, id={}. Item remains PROCESSING for retry.",
                    claimOwner,
                    item.getId()
                );

                failuresRemaining--;
                continue;
            }

            audioIngestionRepository.deleteProcessed(
                item.getId().longValue(),
                claimOwner
            );

            log.info(
                "Successfully processed external audio item. "
                    + "claimOwner={}, id={}",
                claimOwner,
                item.getId()
            );
        }
    }

    private String getClaimOwner() {
        return System.getenv().getOrDefault(
            "HOSTNAME",
            UUID.randomUUID().toString()
        );
    }
}