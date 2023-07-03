package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.UUID;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;

@Service
@RequiredArgsConstructor
public class AudioTransformationServiceImpl implements AudioTransformationService {

    private final MediaRequestService mediaRequestService;
    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final TransientObjectDirectoryService transientObjectDirectoryService;

    @Transactional
    @Override
    public MediaRequestEntity processAudioRequest(Integer requestId) {

        return mediaRequestService.updateAudioRequestStatus(requestId, PROCESSING);
    }

    @Override
    public BinaryData getAudioBlobData(UUID location) {
        return dataManagementService.getBlobData(dataManagementConfiguration.getUnstructuredContainerName(), location);
    }

    @Override
    public UUID saveAudioBlobData(BinaryData binaryData) {
        return dataManagementService.saveBlobData(
            dataManagementConfiguration.getUnstructuredContainerName(),
            binaryData
        );
    }

    @Override
    public TransientObjectDirectoryEntity saveTransientDataLocation(MediaRequestEntity mediaRequest,
                                                                    UUID externalLocation) {
        return transientObjectDirectoryService.saveTransientDataLocation(mediaRequest, externalLocation);
    }

}
