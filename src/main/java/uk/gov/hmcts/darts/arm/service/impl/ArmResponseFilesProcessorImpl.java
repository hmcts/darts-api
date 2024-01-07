package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.storage.blob.models.BlobItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmResponseFilesProcessorImpl implements ArmResponseFilesProcessor {

    public static final String ARM_FILENAME_SEPARATOR = "_";
    public static final String ARM_RESPONSE_FILE_EXTENSION = ".rsp";
    public static final String ARM_INPUT_UPLOAD_FILENAME_KEY = "iu";
    public static final String ARM_CREATE_RECORD_FILENAME_KEY = "cr";
    public static final String ARM_UPLOAD_FILE_FILENAME_KEY = "uf";
    public static final String ARM_RESPONSE_SUCCESS_STATUS_CODE = "1";
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final ArmDataManagementApi armDataManagementApi;


    @Transactional
    @Override
    public void processResponseFiles() {
        // Fetch All records from external Object Directory table with external_location_type as 'ARM' and status with 'ARM dropzone'.
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.ARM.getId());
        ObjectRecordStatusEntity armDropZoneStatus = objectRecordStatusRepository.getReferenceById(ARM_DROP_ZONE.getId());
        ObjectRecordStatusEntity armProcessingResponseFilesStatus = objectRecordStatusRepository.getReferenceById(ARM_PROCESSING_RESPONSE_FILES.getId());
        List<ExternalObjectDirectoryEntity> dataSentToArm =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(armLocation, armDropZoneStatus);

        for (ExternalObjectDirectoryEntity externalObjectDirectory: dataSentToArm) {
            try {
                updateExternalDirectoryObjectStatus(armProcessingResponseFilesStatus, externalObjectDirectory);
                processCollectedFile(externalObjectDirectory);
            } catch (Exception e) {
                updateExternalDirectoryObjectStatus(armDropZoneStatus, externalObjectDirectory);
                log.error("Unable to process response files for external object directory {}", e.getMessage());
            }
        }
    }

    private void updateExternalDirectoryObjectStatus(ObjectRecordStatusEntity objectRecordStatus,
                                                     ExternalObjectDirectoryEntity externalObjectDirectory) {
        externalObjectDirectory.setStatus(objectRecordStatus);
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }

    private void processCollectedFile(ExternalObjectDirectoryEntity externalObjectDirectory) {
        // Using Azure Blob Storage List operation, fetch the filename from
        // dropzone/DARTS/collected using prefix EODID_MEDID_ATTEMPTS for Media of manifest file from Response folder.
        String prefix = getPrefix(externalObjectDirectory);
        log.debug("Checking ARM for files containing name {}", prefix);
        Map<String, BlobItem> collectedBlobs = armDataManagementApi.listCollectedBlobs(prefix);
        if (nonNull(collectedBlobs) && !collectedBlobs.isEmpty()) {
            String armCollectedFilename = collectedBlobs.keySet().stream().findFirst().get();
            log.debug("Found ARM collected file {}", armCollectedFilename);
            if (armCollectedFilename.contains(prefix + ARM_FILENAME_SEPARATOR)) {
                String responseFilesHashcode = armCollectedFilename.substring(armCollectedFilename.indexOf(prefix + ARM_FILENAME_SEPARATOR));
                log.debug("Response files hashcode {}", responseFilesHashcode);
                Map<String, BlobItem> responseBlobs = armDataManagementApi.listResponseBlobs(responseFilesHashcode);
                if (nonNull(responseBlobs) && !responseBlobs.isEmpty()) {
                    processResponseFiles(responseBlobs);
                }
            }
        }
    }

    private void processResponseFiles(Map<String, BlobItem> responseBlobs) {
        String inputUploadFilename = null;
        String createRecordFilename = null;
        String uploadFilename = null;
        for (String responseFile : responseBlobs.keySet()) {
            /* IU - Input Upload - This is the manifest file which gets renamed by ARM.
               -- EODID_MEDID_ATTEMPTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp
               CR - Create Record - This is the create record file which represents record creation in ARM.
               -- 6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp
               UF - Upload File - This is the Upload file which represents the File which is ingested by ARM.
               -- 6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp */
            if (responseFile.endsWith(generateSuffix(ARM_INPUT_UPLOAD_FILENAME_KEY))) {
                inputUploadFilename = responseFile;
            } else if (responseFile.endsWith(generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                createRecordFilename = responseFile;
            } else if (responseFile.endsWith(generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                uploadFilename = responseFile;
            }
        }
        if (nonNull(inputUploadFilename) && nonNull(createRecordFilename) && nonNull(uploadFilename)) {
            UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFilename);
            if (ARM_RESPONSE_SUCCESS_STATUS_CODE.equals(uploadFileFilenameProcessor.getStatus())) {
                armDataManagementApi.getResponseBlobData(uploadFilename);
            }
        }
    }

    private static String generateSuffix(String filenameKey) {
        return ARM_FILENAME_SEPARATOR + filenameKey + ARM_RESPONSE_FILE_EXTENSION;
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
