package uk.gov.hmcts.darts.arm.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;

@Component
@Slf4j
@RequiredArgsConstructor
public class UnstructuredToArmHelper {
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final LogApi logApi;
    private final ArmDataManagementApi armDataManagementApi;
    private final EodHelper eodHelper;


    public List<ExternalObjectDirectoryEntity> getEodEntitiesToSendToArm(ExternalLocationTypeEntity sourceLocation,
                                                                         ExternalLocationTypeEntity armLocation, int maxResultSize) {
        ObjectRecordStatusEntity armRawStatusFailed = objectRecordStatusRepository.getReferenceById(ARM_RAW_DATA_FAILED.getId());
        ObjectRecordStatusEntity armManifestFailed = objectRecordStatusRepository.getReferenceById(ARM_MANIFEST_FAILED.getId());

        List<ObjectRecordStatusEntity> failedArmStatuses = List.of(armRawStatusFailed, armManifestFailed);

        var failedArmExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            failedArmStatuses,
            armLocation,
            armDataManagementConfiguration.getMaxRetryAttempts(),
            Pageable.ofSize(maxResultSize)
        );

        var pendingUnstructuredExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findEodsNotInOtherStorage(
            EodHelper.storedStatus(), sourceLocation,
            EodHelper.armLocation(),
            maxResultSize - failedArmExternalObjectDirectoryEntities.size());

        List<ExternalObjectDirectoryEntity> returnList = new ArrayList<>();
        returnList.addAll(pendingUnstructuredExternalObjectDirectoryEntities);
        returnList.addAll(failedArmExternalObjectDirectoryEntities);
        return returnList;
    }

    public Optional<ExternalObjectDirectoryEntity> getExternalObjectDirectoryEntity(
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity, ExternalLocationTypeEntity eodSourceLocation, ObjectRecordStatusEntity status) {

        return externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            status,
            eodSourceLocation,
            externalObjectDirectoryEntity.getMedia(),
            externalObjectDirectoryEntity.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntity.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntity.getCaseDocument()
        );
    }

    public void updateExternalObjectDirectoryStatusToFailed(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                            ObjectRecordStatusEntity objectRecordStatus, UserAccountEntity userAccount) {
        updateExternalObjectDirectoryStatus(externalObjectDirectoryEntity, objectRecordStatus, userAccount);
    }

    public void incrementTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = ObjectUtils.firstNonNull(externalObjectDirectoryEntity.getTransferAttempts(), 0);
        int newNumberOfAttempts = currentNumberOfAttempts + 1;
        log.debug(
            "Updating failed transfer attempts from {} to {} for ID {}",
            currentNumberOfAttempts,
            newNumberOfAttempts,
            externalObjectDirectoryEntity.getId()
        );
        externalObjectDirectoryEntity.setTransferAttempts(newNumberOfAttempts);
        if (newNumberOfAttempts > armDataManagementConfiguration.getMaxRetryAttempts()) {
            logApi.armPushFailed(externalObjectDirectoryEntity.getId());
        }
    }

    public void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity armExternalObjectDirectory, ObjectRecordStatusEntity armStatus,
                                                    UserAccountEntity userAccount) {
        if (nonNull(armExternalObjectDirectory)) {
            log.debug(
                "Updating ARM status from {} to {} for ID {}",
                armExternalObjectDirectory.getStatus().getDescription(),
                armStatus.getDescription(),
                armExternalObjectDirectory.getId()
            );
            armExternalObjectDirectory.setStatus(armStatus);
            armExternalObjectDirectory.setLastModifiedBy(userAccount);
            externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
        }
    }

    public ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                                ObjectRecordStatusEntity status, UserAccountEntity userAccount) {

        ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(ARM.getId()));
        armExternalObjectDirectoryEntity.setStatus(status);
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        armExternalObjectDirectoryEntity.setVerificationAttempts(1);

        if (nonNull(externalObjectDirectory.getMedia())) {
            armExternalObjectDirectoryEntity.setMedia(externalObjectDirectory.getMedia());
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            armExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(externalObjectDirectory.getTranscriptionDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            armExternalObjectDirectoryEntity.setAnnotationDocumentEntity(externalObjectDirectory.getAnnotationDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {
            armExternalObjectDirectoryEntity.setCaseDocument(externalObjectDirectory.getCaseDocument());
        }
        armExternalObjectDirectoryEntity.setCreatedBy(userAccount);
        armExternalObjectDirectoryEntity.setLastModifiedBy(userAccount);
        armExternalObjectDirectoryEntity.setTransferAttempts(1);

        return armExternalObjectDirectoryEntity;
    }

    public String generateRawFilename(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        final Integer entityId = externalObjectDirectoryEntity.getId();
        final Integer transferAttempts = externalObjectDirectoryEntity.getTransferAttempts();

        Integer documentId = 0;
        if (nonNull(externalObjectDirectoryEntity.getMedia())) {
            documentId = externalObjectDirectoryEntity.getMedia().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getTranscriptionDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getTranscriptionDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getAnnotationDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getAnnotationDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getCaseDocument())) {
            documentId = externalObjectDirectoryEntity.getCaseDocument().getId();
        }

        return String.format("%s_%s_%s", entityId, documentId, transferAttempts);
    }

    public boolean copyRawDataToArm(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
                                    ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                    String filename,
                                    ObjectRecordStatusEntity previousStatus, UserAccountEntity userAccount) {
        try {
            if (previousStatus == null
                || ARM_RAW_DATA_FAILED.getId().equals(previousStatus.getId())
                || ARM_INGESTION.getId().equals(previousStatus.getId())) {
                Instant start = Instant.now();
                log.info("ARM PERFORMANCE PUSH START for EOD {} started at {}", armExternalObjectDirectory.getId(), start);

                log.info("About to push raw data to ARM for EOD {}", armExternalObjectDirectory.getId());
                armDataManagementApi.copyBlobDataToArm(unstructuredExternalObjectDirectory.getExternalLocation().toString(), filename);
                log.info("Pushed raw data to ARM for EOD {}", armExternalObjectDirectory.getId());

                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("ARM PERFORMANCE PUSH END for EOD {} ended at {}", armExternalObjectDirectory.getId(), finish);
                log.info("ARM PERFORMANCE PUSH ELAPSED TIME for EOD {} took {} ms", armExternalObjectDirectory.getId(), timeElapsed);

                armExternalObjectDirectory.setChecksum(unstructuredExternalObjectDirectory.getChecksum());
                armExternalObjectDirectory.setExternalLocation(UUID.randomUUID());
                armExternalObjectDirectory.setLastModifiedBy(userAccount);
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
            }
        } catch (Exception e) {
            log.error(
                "Error copying BLOB data for file {}",
                unstructuredExternalObjectDirectory.getExternalLocation(),
                e
            );
            return false;
        }

        return true;
    }

    public void updateExternalObjectDirectoryFailedTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                                    UserAccountEntity userAccount) {
        incrementTransferAttempts(externalObjectDirectoryEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccount);
        externalObjectDirectoryEntity.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryEntity);
    }
}
