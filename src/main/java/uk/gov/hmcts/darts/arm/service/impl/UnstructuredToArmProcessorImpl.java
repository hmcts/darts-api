package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_ARM_INGESTION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.MARKED_FOR_DELETION;

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
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    @Transactional
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
        ObjectRecordStatusEntity failedArmStatus = objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.FAILURE_ARM_INGESTION_FAILED.getId());
        ObjectRecordStatusEntity armIngestionStatus = objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.ARM_INGESTION.getId());

        List<ObjectRecordStatusEntity> armStatuses = getArmStatuses(storedStatus, failedArmStatus, armIngestionStatus);

        var pendingUnstructuredExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            storedStatus,
            armStatuses,
            inboundLocation,
            armLocation);

        var failedArmExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findFailedNotExceedRetryInStorageLocation(
            failedArmStatus,
            armLocation,
            armDataManagementConfiguration.getMaxRetryAttempts());

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities = Stream.concat(
            pendingUnstructuredExternalObjectDirectoryEntities.stream(),
            failedArmExternalObjectDirectoryEntities.stream()).toList();

        for (var currentExternalObjectDirectoryEntity : allPendingUnstructuredToArmEntities) {

            ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity;
            ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity;

            if (currentExternalObjectDirectoryEntity.getExternalLocationType().getId().equals(armLocation.getId())) {
                armExternalObjectDirectoryEntity = currentExternalObjectDirectoryEntity;

                var matchingEntity = getUnstructuredExternalObjectDirectoryEntity(armExternalObjectDirectoryEntity);
                if (matchingEntity.isPresent()) {
                    unstructuredExternalObjectDirectoryEntity = matchingEntity.get();
                } else {
                    updateTransferAttempts(armExternalObjectDirectoryEntity);
                    externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectoryEntity);
                    log.error("Unable to get external object");
                    continue;
                }
            } else {
                unstructuredExternalObjectDirectoryEntity = currentExternalObjectDirectoryEntity;
                armExternalObjectDirectoryEntity = createArmExternalObjectDirectoryEntity(currentExternalObjectDirectoryEntity);
            }

            armExternalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(ARM_INGESTION.getId()));
            externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectoryEntity);

            copyToArm(unstructuredExternalObjectDirectoryEntity, armExternalObjectDirectoryEntity);

            externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectoryEntity);
        }
    }

    private static List<ObjectRecordStatusEntity> getArmStatuses(ObjectRecordStatusEntity storedStatus,
                                                                 ObjectRecordStatusEntity failedArmStatus,
                                                                 ObjectRecordStatusEntity armIngestionStatus) {
        List<ObjectRecordStatusEntity> armStatuses = new ArrayList<>();
        armStatuses.add(storedStatus);
        armStatuses.add(failedArmStatus);
        armStatuses.add(armIngestionStatus);
        return armStatuses;
    }

    private void copyToArm(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
                           ExternalObjectDirectoryEntity armExternalObjectDirectory) {
        try {
            String filename = generateFilename(armExternalObjectDirectory);
            BinaryData inboundFile = dataManagementApi.getBlobDataFromUnstructuredContainer(
                unstructuredExternalObjectDirectory.getExternalLocation());

            armDataManagementApi.saveBlobDataToArm(filename, inboundFile);
            armExternalObjectDirectory.setChecksum(unstructuredExternalObjectDirectory.getChecksum());
            armExternalObjectDirectory.setExternalLocation(UUID.randomUUID());
            armExternalObjectDirectory.setStatus(objectDirectoryStatusRepository.getReferenceById(MARKED_FOR_DELETION.getId()));

        } catch (BlobStorageException e) {
            log.error("Failed to move BLOB data for file {} due to {}",
                      unstructuredExternalObjectDirectory.getExternalLocation(),
                      e.getMessage());

            armExternalObjectDirectory.setStatus(objectDirectoryStatusRepository.getReferenceById(FAILURE_ARM_INGESTION_FAILED.getId()));
            updateTransferAttempts(armExternalObjectDirectory);
        }

            externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
    }

    private Optional<ExternalObjectDirectoryEntity> getUnstructuredExternalObjectDirectoryEntity(
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        return externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            objectDirectoryStatusRepository.getReferenceById(ObjectDirectoryStatusEnum.STORED.getId()),
            externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()),
            externalObjectDirectoryEntity.getMedia(),
            externalObjectDirectoryEntity.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntity.getAnnotationDocumentEntity());
    }

    private ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {

        ExternalObjectDirectoryEntity  armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(ARM.getId()));
        armExternalObjectDirectoryEntity.setStatus(objectDirectoryStatusRepository.getReferenceById(ARM_INGESTION.getId()));
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());

        if (nonNull(externalObjectDirectory.getMedia())) {
            armExternalObjectDirectoryEntity.setMedia(externalObjectDirectory.getMedia());
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            armExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(externalObjectDirectory.getTranscriptionDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            armExternalObjectDirectoryEntity.setAnnotationDocumentEntity(externalObjectDirectory.getAnnotationDocumentEntity());
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
        if (nonNull(externalObjectDirectoryEntity.getMedia())) {
            documentId = externalObjectDirectoryEntity.getMedia().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getTranscriptionDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getTranscriptionDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getAnnotationDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getAnnotationDocumentEntity().getId();
        }

        return String.format("{}_{}_{}",
                             Integer.toString(entityId),
                             Integer.toString(documentId),
                             Integer.toString(transferAttempts));
    }

    private void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        externalObjectDirectoryEntity.setTransferAttempts(currentNumberOfAttempts + 1);
    }

}
