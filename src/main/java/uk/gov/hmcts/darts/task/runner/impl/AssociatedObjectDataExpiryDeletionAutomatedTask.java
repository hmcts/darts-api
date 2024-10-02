package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.runner.SoftDelete;
import uk.gov.hmcts.darts.task.runner.SoftDeleteRepository;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
public class AssociatedObjectDataExpiryDeletionAutomatedTask
    extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;

    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final MediaRepository mediaRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;

    public AssociatedObjectDataExpiryDeletionAutomatedTask(
        AutomatedTaskRepository automatedTaskRepository,
        AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
        UserIdentity userIdentity,
        LogApi logApi, LockService lockService,
        CurrentTimeHelper currentTimeHelper,
        TranscriptionDocumentRepository transcriptionDocumentRepository, MediaRepository mediaRepository,
        AnnotationDocumentRepository annotationDocumentRepository,
        CaseDocumentRepository caseDocumentRepository,
        ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
        ExternalInboundDataStoreDeleter inboundDeleter,
        ExternalUnstructuredDataStoreDeleter unstructuredDeleter) {

        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.userIdentity = userIdentity;
        this.currentTimeHelper = currentTimeHelper;
        this.transcriptionDocumentRepository = transcriptionDocumentRepository;
        this.mediaRepository = mediaRepository;
        this.annotationDocumentRepository = annotationDocumentRepository;
        this.caseDocumentRepository = caseDocumentRepository;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.inboundDeleter = inboundDeleter;
        this.unstructuredDeleter = unstructuredDeleter;
    }


    @Override
    @Transactional
    public void run() {
        super.run();
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.ASSOCIATED_OBJECT_DATA_EXPIRY_DELETION_TASK_NAME;
    }

    @Override
    public void runTask() {
        final UserAccountEntity userAccount = userIdentity.getUserAccount();
        OffsetDateTime maxRetentionDate = currentTimeHelper.currentOffsetDateTime();
        Limit batchSize = Limit.of(getAutomatedTaskBatchSize());

        deleteTranscriptionDocumentEntity(userAccount, maxRetentionDate, batchSize);
        deleteMediaEntity(userAccount, maxRetentionDate, batchSize);
        deleteAnnotationDocumentEntity(userAccount, maxRetentionDate, batchSize);
        deleteCaseDocumentEntity(userAccount, maxRetentionDate, batchSize);
    }

    void deleteTranscriptionDocumentEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit batchSize) {
        deleteExternalObjectDirectoryEntity(
            userAccount,
            transcriptionDocumentRepository,
            externalObjectDirectoryRepository.findExpiredTranscriptionDocuments(maxRetentionDate, batchSize),
            ExternalObjectDirectoryEntity::getTranscriptionDocumentEntity
        );
    }


    void deleteMediaEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit batchSize) {
        deleteExternalObjectDirectoryEntity(
            userAccount,
            mediaRepository,
            externalObjectDirectoryRepository.findExpiredMediaEntries(maxRetentionDate, batchSize),
            ExternalObjectDirectoryEntity::getMedia
        );
    }

    void deleteAnnotationDocumentEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit batchSize) {
        deleteExternalObjectDirectoryEntity(
            userAccount,
            annotationDocumentRepository,
            externalObjectDirectoryRepository.findExpiredAnnotationDocuments(maxRetentionDate, batchSize),
            ExternalObjectDirectoryEntity::getAnnotationDocumentEntity
        );
    }

    void deleteCaseDocumentEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit batchSize) {
        deleteExternalObjectDirectoryEntity(
            userAccount,
            caseDocumentRepository,
            externalObjectDirectoryRepository.findExpiredCaseDocuments(maxRetentionDate, batchSize),
            ExternalObjectDirectoryEntity::getCaseDocument
        );
    }


    <T extends SoftDelete> void deleteExternalObjectDirectoryEntity(
        UserAccountEntity userAccount,
        SoftDeleteRepository<T, ?> repository,
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
        Function<ExternalObjectDirectoryEntity, T> entityMapper) {


        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesToDelete = externalObjectDirectoryEntities
            .stream()
            .filter(this::deleteFromExternalDataStore)
            .toList();

        List<T> entitiesToDelete = externalObjectDirectoryEntitiesToDelete
            .stream()
            .map(entityMapper)
            .toList();

        externalObjectDirectoryRepository.deleteAll(externalObjectDirectoryEntitiesToDelete);
        repository.softDeleteAll(entitiesToDelete, userAccount);
    }

    boolean deleteFromExternalDataStore(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        if (ExternalLocationTypeEnum.INBOUND.getId().equals(externalObjectDirectoryEntity.getExternalLocationType().getId())) {
            return inboundDeleter.delete(externalObjectDirectoryEntity);
        } else if (ExternalLocationTypeEnum.UNSTRUCTURED.getId().equals(externalObjectDirectoryEntity.getExternalLocationType().getId())) {
            return unstructuredDeleter.delete(externalObjectDirectoryEntity);
        } else if (ExternalLocationTypeEnum.ARM.getId().equals(externalObjectDirectoryEntity.getExternalLocationType().getId())) {
            return true;//Do nothing
        } else {
            log.error("Can only delete INBOUND (1), UNSTRUCTURED (2), ARM(3) data but tried to delete : {}",
                      externalObjectDirectoryEntity.getExternalLocationType().getId());
            return false;
        }
    }
}
