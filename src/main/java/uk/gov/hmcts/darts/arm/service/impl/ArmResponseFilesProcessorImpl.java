package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.storage.blob.models.BlobItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmResponseFilesProcessorImpl implements ArmResponseFilesProcessor {

    public static final String ARM_FILENAME_SEPARATOR = "_";
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final ArmDataManagementApi armDataManagementApi;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    @Override
    public void processResponseFiles() {
        // Fetch All records from external Object Directory table with external_location_type as 'ARM' and status with 'ARM dropzone'.
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.ARM.getId());
        ObjectRecordStatusEntity armDropZoneStatus = objectRecordStatusRepository.getReferenceById(ARM_INGESTION.getId());
        List<ExternalObjectDirectoryEntity> dataSentToArm =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(armLocation, armDropZoneStatus);

        for (ExternalObjectDirectoryEntity externalObjectDirectory: dataSentToArm) {
            // Using Azure Blob Storage List operation, fetch the filename from
            // dropzone/DARTS/collected using prefix EODID_MEDID_ATTEMPTS for Media of manifest file from Response folder.
            String prefix = getPrefix(externalObjectDirectory);
            Map<String, BlobItem> collectedBlobs = armDataManagementApi.listCollectedBlobs(prefix);

        }
    }

    private static String getPrefix(ExternalObjectDirectoryEntity externalObjectDirectory) {
        StringBuilder prefix = new StringBuilder(externalObjectDirectory.getId().toString())
            .append(ARM_FILENAME_SEPARATOR)
            .append(getObjectTypeId(externalObjectDirectory))
            .append(ARM_FILENAME_SEPARATOR)
            .append(externalObjectDirectory.getTransferAttempts());

        return prefix.toString();
    }

    private static String getObjectTypeId(ExternalObjectDirectoryEntity externalObjectDirectory) {
        if (nonNull(externalObjectDirectory.getMedia())) {
            return externalObjectDirectory.getMedia().getId().toString();
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            return externalObjectDirectory.getTranscriptionDocumentEntity().getId().toString();
        } else if(nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            return externalObjectDirectory.getAnnotationDocumentEntity().getId().toString();
        }
        return null;
    }


}
