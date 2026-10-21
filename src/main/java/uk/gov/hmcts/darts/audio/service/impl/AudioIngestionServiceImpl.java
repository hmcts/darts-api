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

        List<AudioIngestionEntity> claimedItems =
            audioIngestionRepository.findAllById(claimedIds);

        log.info(
            "External audio ingestion job claimed {} items. "
                + "claimOwner={}, ids={}",
            claimedIds.size(),
            claimOwner,
            claimedIds
        );

        processItems(
            claimedItems,
            claimOwner
        );
    }

    private void processItems(
        List<AudioIngestionEntity> claimedItems,
        String claimOwner
    ) {

        for (AudioIngestionEntity item : claimedItems) {
            String filename = item.getSourceFullFilename();
            log.info(
                "Processing external audio item. "
                    + "claimOwner={}, id={}, sourceFullFilename={}",
                claimOwner,
                item.getId(),
                filename
            );

            if (filename != null
                && java.nio.file.Path.of(filename)
                .getFileName()
                .toString()
                .startsWith("FAIL")) {
                log.warn(
                    "Simulating temporary failure for external audio item. "
                        + "claimOwner={}, id={}, filename={}. "
                        + "Item remains PROCESSING for retry.",
                    claimOwner,
                    item.getId(),
                    filename
                );

                continue;
            }

            int deleted = audioIngestionRepository.deleteProcessed(
                item.getId().longValue(),
                claimOwner
            );

            log.info(
                "External audio item processing completed. "
                    + "claimOwner={}, id={}, deleted={}",
                claimOwner,
                item.getId(),
                deleted
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