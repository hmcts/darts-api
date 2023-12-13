package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.service.UnstructuredToArmProcessor;

import java.time.OffsetDateTime;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnstructuredToArmProcessorImpl implements UnstructuredToArmProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final DataManagementApi dataManagementApi;
    private final UserAccountRepository userAccountRepository;

    @Override
    public void processUnstructuredToArm() {
    }

    private void processPending() {

        ObjectRecordStatusEntity storedStatus = objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.STORED.getId());
        ExternalLocationTypeEntity inboundLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.ARM.getId());

        var pendingUnstructuredExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            storedStatus,
            storedStatus,
            inboundLocation,
            armLocation
        );

        for (var unstructuredExternalObjectDirectoryEntity : pendingUnstructuredExternalObjectDirectoryEntities) {
                ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity =
                    createArmExternalObjectDirectoryEntity(unstructuredExternalObjectDirectoryEntity);

                armExternalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(ARM_INGESTION.getId()));
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectoryEntity);

            try {
                BinaryData inboundFile = dataManagementApi.getBlobDataFromUnstructuredContainer(unstructuredExternalObjectDirectoryEntity.getExternalLocation());

                UUID uuid = dataManagementApi.saveBlobDataToARM(inboundFile);
                armExternalObjectDirectoryEntity.setChecksum(unstructuredExternalObjectDirectoryEntity.getChecksum());
                armExternalObjectDirectoryEntity.setExternalLocation(uuid);
                armExternalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(STORED.getId()));

            } catch (BlobStorageException e) {
                log.error("Failed to get BLOB from datastore for file {}", unstructuredExternalObjectDirectoryEntity.getExternalLocation());
                armExternalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(FAILURE_FILE_NOT_FOUND.getId()));
                updateTransferAttempts(armExternalObjectDirectoryEntity);
            }

            externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
        }
    }

    private ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {

        ExternalObjectDirectoryEntity  armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(ARM.getId()));
        armExternalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(ARM_INGESTION.getId()));
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());

        MediaEntity mediaEntity = externalObjectDirectory.getMedia();
        if (mediaEntity != null) {
            armExternalObjectDirectoryEntity.setMedia(mediaEntity);
        }

        TranscriptionDocumentEntity transcriptionDocumentEntity = externalObjectDirectory.getTranscriptionDocumentEntity();
        if (transcriptionDocumentEntity != null) {
            armExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        }

        AnnotationDocumentEntity annotationDocumentEntity = externalObjectDirectory.getAnnotationDocumentEntity();
        if (annotationDocumentEntity != null) {
            armExternalObjectDirectoryEntity.setAnnotationDocumentEntity(annotationDocumentEntity);
        }
        OffsetDateTime now = OffsetDateTime.now();
        armExternalObjectDirectoryEntity.setCreatedDateTime(now);
        armExternalObjectDirectoryEntity.setLastModifiedDateTime(now);
        var systemUser = userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
        armExternalObjectDirectoryEntity.setCreatedBy(systemUser);
        armExternalObjectDirectoryEntity.setLastModifiedBy(systemUser);
        armExternalObjectDirectoryEntity.setTransferAttempts(1);

        return armExternalObjectDirectoryEntity;
    }

    private void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        externalObjectDirectoryEntity.setTransferAttempts(currentNumberOfAttempts++);
    }

}
