package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;

@Slf4j
public abstract class AbstractUnstructuredToArmProcessor implements UnstructuredToArmProcessor {

    protected static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;
    protected final ObjectRecordStatusRepository objectRecordStatusRepository;
    protected final UserIdentity userIdentity;
    protected final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    protected final ExternalLocationTypeRepository externalLocationTypeRepository;
    protected final DataManagementApi dataManagementApi;
    protected final ArmDataManagementApi armDataManagementApi;
    protected final FileOperationService fileOperationService;
    protected final LogApi logApi;
    protected final ArmDataManagementConfiguration armDataManagementConfiguration;


    protected UserAccountEntity userAccount;


    protected final Integer batchSize;

    protected AbstractUnstructuredToArmProcessor(ObjectRecordStatusRepository objectRecordStatusRepository,
                                                 UserIdentity userIdentity,
                                                 ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                 ExternalLocationTypeRepository externalLocationTypeRepository,
                                                 DataManagementApi dataManagementApi,
                                                 ArmDataManagementApi armDataManagementApi,
                                                 FileOperationService fileOperationService,
                                                 Integer batchSize,
                                                 LogApi logApi,
                                                 ArmDataManagementConfiguration armDataManagementConfiguration) {
        this.objectRecordStatusRepository = objectRecordStatusRepository;
        this.userIdentity = userIdentity;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.externalLocationTypeRepository = externalLocationTypeRepository;
        this.dataManagementApi = dataManagementApi;
        this.armDataManagementApi = armDataManagementApi;
        this.fileOperationService = fileOperationService;
        this.batchSize = batchSize;
        this.logApi = logApi;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
    }

    protected ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                                   ObjectRecordStatusEntity status) {

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
        OffsetDateTime now = OffsetDateTime.now();
        armExternalObjectDirectoryEntity.setCreatedDateTime(now);
        armExternalObjectDirectoryEntity.setLastModifiedDateTime(now);
        var systemUser = userIdentity.getUserAccount();
        armExternalObjectDirectoryEntity.setCreatedBy(systemUser);
        armExternalObjectDirectoryEntity.setLastModifiedBy(systemUser);
        armExternalObjectDirectoryEntity.setTransferAttempts(1);

        return armExternalObjectDirectoryEntity;
    }

    protected void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity armExternalObjectDirectory, ObjectRecordStatusEntity armStatus) {
        if (nonNull(armExternalObjectDirectory)) {
            log.debug(
                "Updating ARM status from {} to {} for ID {}",
                armExternalObjectDirectory.getStatus().getDescription(),
                armStatus.getDescription(),
                armExternalObjectDirectory.getId()
            );
            armExternalObjectDirectory.setStatus(armStatus);
            armExternalObjectDirectory.setLastModifiedBy(userAccount);
            armExternalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
            externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
        }
    }

    protected void updateExternalObjectDirectoryStatusToFailed(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                               ObjectRecordStatusEntity objectRecordStatus) {
        updateTransferAttempts(externalObjectDirectoryEntity);
        updateExternalObjectDirectoryStatus(externalObjectDirectoryEntity, objectRecordStatus);
    }

    protected void updateExternalObjectDirectoryFailedTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        updateTransferAttempts(externalObjectDirectoryEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccount);
        externalObjectDirectoryEntity.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryEntity);
    }

    protected Optional<ExternalObjectDirectoryEntity> getExternalObjectDirectoryEntity(
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

    protected void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        log.debug(
            "Updating failed transfer attempts from {} to {} for ID {}",
            currentNumberOfAttempts,
            currentNumberOfAttempts + 1,
            externalObjectDirectoryEntity.getId()
        );
        externalObjectDirectoryEntity.setTransferAttempts(++currentNumberOfAttempts);
        if (currentNumberOfAttempts > armDataManagementConfiguration.getMaxRetryAttempts()) {
            logApi.armPushFailed(externalObjectDirectoryEntity.getId());
        }
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

    protected boolean copyRawDataToArm(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
                                       ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                       String filename,
                                       ObjectRecordStatusEntity previousStatus,
                                       Runnable recoveryAction) {
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
                armExternalObjectDirectory.setLastModifiedBy(userIdentity.getUserAccount());
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
            }
        } catch (Exception e) {
            log.error(
                "Error copying BLOB data for file {}",
                unstructuredExternalObjectDirectory.getExternalLocation(),
                e
            );
            recoveryAction.run();
            return false;
        }

        return true;
    }

}