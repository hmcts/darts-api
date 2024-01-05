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
            /* IU - Input Upload - This is the manifest file which gets renamed by ARM.
               CR - Create Record - This is the create record file which represents record creation in ARM.
               UF - Upload File - This is the Upload file which represents the File which is ingested by ARM.
               -- EODID_MEDID_ATTEMPTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp
               -- 6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp
               -- 6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp */
            String prefix = getPrefix(externalObjectDirectory);
            log.debug("Looking for files containing name {}", prefix);
            Map<String, BlobItem> collectedBlobs = armDataManagementApi.listCollectedBlobs(prefix);
            if (nonNull(collectedBlobs) && !collectedBlobs.isEmpty()) {
                String inputUploadFilename = collectedBlobs.keySet().stream().findFirst().get();
                log.debug("Found Input upload file {}", inputUploadFilename);
                if (inputUploadFilename.contains(prefix+ARM_FILENAME_SEPARATOR)) {
                    String inputUploadFilenameMinusPrefix = inputUploadFilename.substring(inputUploadFilename.indexOf(prefix+ARM_FILENAME_SEPARATOR));
                    log.debug("Stripped input upload file {}", inputUploadFilenameMinusPrefix);
                    
                }
            }
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
        String objectTypeId = "";
        if (nonNull(externalObjectDirectory.getMedia())) {
            objectTypeId = externalObjectDirectory.getMedia().getId().toString();
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            objectTypeId = externalObjectDirectory.getTranscriptionDocumentEntity().getId().toString();
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            objectTypeId = externalObjectDirectory.getAnnotationDocumentEntity().getId().toString();
        }
        return objectTypeId;
    }


}
