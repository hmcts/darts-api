package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioIngestionConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.AudioIngestionEntity;
import uk.gov.hmcts.darts.audio.service.AudioIngestionService;
import uk.gov.hmcts.darts.common.repository.AudioIngestionRepository;

import java.time.OffsetDateTime;
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
            OffsetDateTime.now().minus(config.getProcessingTimeout());


        List<Long> claimedIds =
            audioIngestionRepository.claimForProcessing(claimOwner, cutoff, config.getBatchSize());

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
            "External audio ingestion job claimed {} items. claimOwner={}, ids={}",
            claimedItems.size(),
            claimOwner,
            claimedIds
        );

        claimedItems.forEach(item ->
                                 log.info(
                                     "Would process external audio item. "
                                         + "claimOwner={}, id={}, sourceFullFilename={}",
                                     claimOwner,
                                     item.getId(),
                                     item.getSourceFullFilename()
                                 )
        );
    }

    private String getClaimOwner() {
        return System.getenv().getOrDefault(
            "HOSTNAME",
            UUID.randomUUID().toString()
        );
    }
}