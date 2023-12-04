package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
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
    private final ArmDataManagementApi armDataManagementApi;
    private final UserAccountRepository userAccountRepository;

    @Override
    public void processUnstructuredToArm() {
        processPendingUnstructured();
    }

    private void processPendingUnstructured() {

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
                String filename = generateFilename(armExternalObjectDirectoryEntity);
                BinaryData inboundFile = dataManagementApi
                    .getBlobDataFromUnstructuredContainer(unstructuredExternalObjectDirectoryEntity.getExternalLocation());

                String blobName = armDataManagementApi.saveBlobDataToArm(filename, inboundFile);
                armExternalObjectDirectoryEntity.setChecksum(unstructuredExternalObjectDirectoryEntity.getChecksum());
                armExternalObjectDirectoryEntity.setExternalLocation(UUID.randomUUID()); //TODO: change to use filename not UUID???
                armExternalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(STORED.getId()));

            } catch (BlobStorageException e) {
                log.error("Failed to move BLOB data for file {} due to {}",
                          unstructuredExternalObjectDirectoryEntity.getExternalLocation(),
                          e.getMessage());

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

    @Override
    public String generateFilename(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        final Integer entityId = externalObjectDirectoryEntity.getId();
        final Integer transferAttempts = externalObjectDirectoryEntity.getTransferAttempts();

        Integer documentId = 0;
        if (externalObjectDirectoryEntity.getMedia() != null) {
            documentId = externalObjectDirectoryEntity.getMedia().getId();
        }
        if (externalObjectDirectoryEntity.getTranscriptionDocumentEntity() != null) {
            documentId = externalObjectDirectoryEntity.getTranscriptionDocumentEntity().getId();
        }
        if (externalObjectDirectoryEntity.getAnnotationDocumentEntity() != null) {
            documentId = externalObjectDirectoryEntity.getAnnotationDocumentEntity().getId();
        }

        return String.format("{}_{}_{}",
                             Integer.toString(entityId),
                             Integer.toString(documentId),
                             Integer.toString(transferAttempts));
    }

    private void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        externalObjectDirectoryEntity.setTransferAttempts(currentNumberOfAttempts++);
    }

}
