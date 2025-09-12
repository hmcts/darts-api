package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.datamanagement.service.AssociatedObjectDataExpiryDeleterService;
import uk.gov.hmcts.darts.task.config.AssociatedObjectDataExpiryDeletionAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.CanReturnExternalObjectDirectoryEntities;
import uk.gov.hmcts.darts.task.runner.HasId;
import uk.gov.hmcts.darts.task.runner.HasRetention;
import uk.gov.hmcts.darts.task.runner.SoftDelete;
import uk.gov.hmcts.darts.task.runner.SoftDeleteRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects")//TODO - refactor to reduce coupling when this class is next edited
public class AssociatedObjectDataExpiryDeleterServiceImpl implements AssociatedObjectDataExpiryDeleterService {
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final MediaRepository mediaRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final AssociatedObjectDataExpiryDeletionAutomatedTaskConfig config;
    private final TransactionTemplate transactionTemplate;

    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;
    private final AuditApi auditApi;

    @Value("${darts.storage.arm.event-date-adjustment-years}")
    Integer eventDateAdjustmentYears;

    @SuppressWarnings("PMD.ExcessiveParameterList")
    public AssociatedObjectDataExpiryDeleterServiceImpl(UserIdentity userIdentity,
                                                        CurrentTimeHelper currentTimeHelper,
                                                        TranscriptionDocumentRepository transcriptionDocumentRepository,
                                                        MediaRepository mediaRepository,
                                                        AnnotationDocumentRepository annotationDocumentRepository,
                                                        CaseDocumentRepository caseDocumentRepository,
                                                        ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                        AssociatedObjectDataExpiryDeletionAutomatedTaskConfig config,
                                                        TransactionTemplate transactionTemplate,
                                                        ExternalInboundDataStoreDeleter inboundDeleter,
                                                        ExternalUnstructuredDataStoreDeleter unstructuredDeleter,
                                                        AuditApi auditApi
    ) {
        this.userIdentity = userIdentity;
        this.currentTimeHelper = currentTimeHelper;
        this.transcriptionDocumentRepository = transcriptionDocumentRepository;
        this.mediaRepository = mediaRepository;
        this.annotationDocumentRepository = annotationDocumentRepository;
        this.caseDocumentRepository = caseDocumentRepository;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.config = config;
        this.transactionTemplate = transactionTemplate;
        this.inboundDeleter = inboundDeleter;
        this.unstructuredDeleter = unstructuredDeleter;
        this.auditApi = auditApi;
    }

    @Transactional
    @Override
    public void delete(Integer batchSize) {
        final UserAccountEntity userAccount = userIdentity.getUserAccount();
        OffsetDateTime maxRetentionDate = currentTimeHelper.currentOffsetDateTime()
            .minus(config.getBufferDuration());

        Limit limit = Limit.of(batchSize);

        deleteTranscriptionDocumentEntity(userAccount, maxRetentionDate, limit);
        deleteMediaEntity(userAccount, maxRetentionDate, limit);
        deleteAnnotationDocumentEntity(userAccount, maxRetentionDate, limit);
        deleteCaseDocumentEntity(userAccount, maxRetentionDate, limit);
    }

    void deleteTranscriptionDocumentEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit limit) {
        log.info("Deleting transcription documents with retention date older than {} limited to batch size {}", maxRetentionDate, limit.max());
        transactionTemplate.executeWithoutResult(status -> deleteExternalObjectDirectoryEntity(
                                                     userAccount,
                                                     transcriptionDocumentRepository,
                                                     externalObjectDirectoryRepository.findExpiredTranscriptionDocuments(maxRetentionDate, limit),
                                                     ExternalObjectDirectoryEntity::getTranscriptionDocumentEntity,
                                                     AuditActivity.TRANSCRIPT_EXPIRED
                                                 )
        );
    }


    void deleteMediaEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit limit) {
        log.info("Deleting media with retention date older than {} limited to batch size {}", maxRetentionDate, limit.max());
        transactionTemplate.executeWithoutResult(status -> deleteExternalObjectDirectoryEntity(
                                                     userAccount,
                                                     mediaRepository,
                                                     externalObjectDirectoryRepository.findExpiredMediaEntries(maxRetentionDate, limit),
                                                     ExternalObjectDirectoryEntity::getMedia,
                                                     AuditActivity.AUDIO_EXPIRED
                                                 )
        );
    }

    void deleteAnnotationDocumentEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit limit) {
        log.info("Deleting annotation documents with retention date older than {} limited to batch size {}", maxRetentionDate, limit.max());
        transactionTemplate.executeWithoutResult(status -> deleteExternalObjectDirectoryEntity(
                                                     userAccount,
                                                     annotationDocumentRepository,
                                                     externalObjectDirectoryRepository.findExpiredAnnotationDocuments(maxRetentionDate, limit),
                                                     ExternalObjectDirectoryEntity::getAnnotationDocumentEntity,
                                                     AuditActivity.ANNOTATION_EXPIRED
                                                 )
        );
    }

    void deleteCaseDocumentEntity(UserAccountEntity userAccount, OffsetDateTime maxRetentionDate, Limit limit) {
        log.info("Deleting case documents with retention date older than {} limited to batch size {}", maxRetentionDate, limit.max());
        transactionTemplate.executeWithoutResult(status -> deleteExternalObjectDirectoryEntity(
                                                     userAccount,
                                                     caseDocumentRepository,
                                                     externalObjectDirectoryRepository.findExpiredCaseDocuments(maxRetentionDate, limit),
                                                     ExternalObjectDirectoryEntity::getCaseDocument,
                                                     AuditActivity.CASE_DOCUMENT_EXPIRED
                                                 )
        );
    }


    <T extends SoftDelete & HasId<?> & HasRetention & CanReturnExternalObjectDirectoryEntities> void deleteExternalObjectDirectoryEntity(
        UserAccountEntity userAccount,
        SoftDeleteRepository<T, ?> repository,
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
        Function<ExternalObjectDirectoryEntity, T> entityMapper,
        AuditActivity auditActivity) {

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesToDelete = externalObjectDirectoryEntities
            .stream()
            .filter(externalObjectDirectoryEntity -> shouldDeleteFilter(entityMapper.apply(externalObjectDirectoryEntity)))
            .filter(this::deleteFromExternalDataStore)
            .toList();

        List<T> entitiesToDelete = externalObjectDirectoryEntitiesToDelete
            .stream()
            .map(entityMapper)
            .distinct()
            .toList();

        externalObjectDirectoryRepository.deleteAll(externalObjectDirectoryEntitiesToDelete);
        repository.softDeleteAll(entitiesToDelete, userAccount);
        entitiesToDelete.forEach(t -> auditApi.record(auditActivity, userAccount, String.valueOf(t.getId())));
    }

    <T extends SoftDelete & HasId<?> & HasRetention & CanReturnExternalObjectDirectoryEntities> boolean shouldDeleteFilter(T entity) {
        ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity = entity.getExternalObjectDirectoryEntities()
            .stream()
            .filter(e -> ExternalLocationTypeEnum.ARM.getId().equals(e.getExternalLocationType().getId()))
            .findFirst()
            .orElse(null);

        if (armExternalObjectDirectoryEntity == null) {
            log.info("Skipping deletion of {} with id {} as there is no ARM external object directory entity",
                     entity.getClass().getSimpleName(),
                     entity.getId());
            return false;
        }

        if (armExternalObjectDirectoryEntity.getEventDateTs() == null
            || !armExternalObjectDirectoryEntity.getEventDateTs().toLocalDate().plusYears(eventDateAdjustmentYears)
            .isEqual(entity.getRetainUntilTs().toLocalDate())) {
            log.info("Skipping deletion of {} with id '{}' as the event date ({}) plus '{}' years is not the same as the retention date ({})",
                     entity.getClass().getSimpleName(),
                     entity.getId(),
                     Optional.ofNullable(armExternalObjectDirectoryEntity.getEventDateTs())
                         .map(OffsetDateTime::toLocalDate)
                         .orElse(null),
                     eventDateAdjustmentYears,
                     entity.getRetainUntilTs().toLocalDate()
            );
            return false;
        }
        return true;
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
