package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.InboundAudioFailureCorrectionService;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundAudioFailureCorrectionServiceImpl implements InboundAudioFailureCorrectionService {

    // Define the maximum number of attempts allowed for correction
    public static final int MAX_ATTEMPTS = 3;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;

    @Override
    public void correctAudioFailure(int batchSize) {
        log.info("Correcting {} inbound audio files with failure out of a batch size {}", batchSize, batchSize);

        List<ExternalObjectDirectoryEntity> failedEods = externalObjectDirectoryRepository.findFailedAudiosWithMaxAttempts(
            EodHelper.inboundLocation(),
            EodHelper.failureStatus(),
            MAX_ATTEMPTS,
            batchSize
        );

        log.info("Total number of inbound audio files with failure {} out of a batch size {}", failedEods.size(), batchSize);

        for (ExternalObjectDirectoryEntity eod : failedEods) {
            try {
                restoreAudioLocation(eod);
            } catch (Exception ex) {
                log.error("Failed to correct inbound audio file with EOD ID: {}. Error: {}", eod.getId(), ex.getMessage());
            }
        }
    }

    private void restoreAudioLocation(ExternalObjectDirectoryEntity eod) {
        if (nonNull(eod.getExternalLocation())) {
            log.info("Restoring audio file with EOD ID: {} to original location: {}", eod.getId(), eod.getExternalLocation());
            String inboundExternalLocation = eod.getExternalLocation();
            dataManagementService.restoreBlobVersion(dataManagementConfiguration.getInboundContainerName(), inboundExternalLocation);
        } else {
            log.warn("EOD ID: {} has no external location to restore.", eod.getId());
        }
    }
}
