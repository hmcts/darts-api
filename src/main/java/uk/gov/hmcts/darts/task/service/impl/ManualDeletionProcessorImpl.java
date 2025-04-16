package uk.gov.hmcts.darts.task.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.model.RetConfReason;
import uk.gov.hmcts.darts.task.runner.SoftDelete;
import uk.gov.hmcts.darts.task.service.ManualDeletionProcessor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class ManualDeletionProcessorImpl implements ManualDeletionProcessor {

    private final UserIdentity userIdentity;
    private final ObjectAdminActionEntityProcessor objectAdminActionEntityProcessor;
    private final ObjectAdminActionRepository objectAdminActionRepository;

    @Value("${darts.manual-deletion.grace-period:24h}")
    private Duration gracePeriod;

    @Override
    public void process(Integer batchSize) {
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        OffsetDateTime deletionThreshold = getDeletionThreshold();
        List<Integer> actionsToDelete = objectAdminActionRepository.findObjectAdminActionsIdsForManualDeletion(deletionThreshold, Limit.of(batchSize));
        log.info("Found {} ObjectAdminActionEntities to delete out of batch size {}", actionsToDelete.size(), batchSize);
        for (Integer objectAdminActionId : actionsToDelete) {
            try {
                objectAdminActionEntityProcessor.processObjectAdminActionEntity(userAccount, objectAdminActionId);
            } catch (Exception e) {
                log.error("Error while processing ObjectAdminActionEntity with ID: {}", objectAdminActionId, e);
            }
        }
    }

    OffsetDateTime getDeletionThreshold() {
        return OffsetDateTime.now().minus(gracePeriod);
    }

    @Service
    @RequiredArgsConstructor
    //Nested class to ensure transactions work as expected
    public static class ObjectAdminActionEntityProcessor {

        private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
        private final ObjectAdminActionRepository objectAdminActionRepository;
        private final MediaRepository mediaRepository;
        private final TranscriptionDocumentRepository transcriptionDocumentRepository;
        private final ExternalInboundDataStoreDeleter inboundDeleter;
        private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;
        private final LogApi logApi;
        private final ArmDataManagementApi armDataManagementApi;
        private final ObjectMapper objectMapper;
        @Value("${darts.storage.arm.event-date-adjustment-years}")
        private Integer eventDateAdjustmentYears;


        @Transactional
        void processObjectAdminActionEntity(UserAccountEntity userAccount, Integer id) {
            ObjectAdminActionEntity objectAdminAction = objectAdminActionRepository.findById(id)
                .orElseThrow(() -> new DartsException("ObjectAdminActionEntity not found with ID: " + id));

            if (isNotDeleted(objectAdminAction.getMedia())) {
                deleteMedia(userAccount, objectAdminAction);
            } else if (isNotDeleted(objectAdminAction.getTranscriptionDocument())) {
                deleteTranscriptionDocument(userAccount, objectAdminAction);
            }
        }

        void deleteMedia(UserAccountEntity userAccount, ObjectAdminActionEntity objectAdminAction) {
            MediaEntity mediaEntity = objectAdminAction.getMedia();
            log.info("Deleting mediaEntity with ID: {}", mediaEntity.getId());
            List<ExternalObjectDirectoryEntity> objectsToDelete = externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByMediaId(
                mediaEntity.getId());

            for (ExternalObjectDirectoryEntity externalObjectDirectoryEntity : objectsToDelete) {
                deleteFromExternalDataStore(externalObjectDirectoryEntity);
                externalObjectDirectoryRepository.delete(externalObjectDirectoryEntity);
            }

            mediaEntity.markAsDeleted(userAccount);
            mediaRepository.save(mediaEntity);
            processArmEods(mediaEntity.getDeletedTs(),
                           objectAdminAction,
                           externalObjectDirectoryRepository.findByMediaAndExternalLocationTypeAndStatus(mediaEntity,
                                                                                                         EodHelper.armLocation(),
                                                                                                         EodHelper.storedStatus()));
            logApi.mediaDeleted(mediaEntity.getId());
        }

        void deleteTranscriptionDocument(UserAccountEntity userAccount, ObjectAdminActionEntity objectAdminAction) {
            TranscriptionDocumentEntity transcription = objectAdminAction.getTranscriptionDocument();
            log.info("Deleting transcription document with ID: {}", transcription.getId());

            List<ExternalObjectDirectoryEntity> objectsToDelete =
                externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByTranscriptionId(transcription.getId());

            for (ExternalObjectDirectoryEntity externalObjectDirectoryEntity : objectsToDelete) {
                deleteFromExternalDataStore(externalObjectDirectoryEntity);
                externalObjectDirectoryRepository.delete(externalObjectDirectoryEntity);
            }
            transcription.markAsDeleted(userAccount);
            transcriptionDocumentRepository.save(transcription);
            processArmEods(transcription.getDeletedTs(),
                           objectAdminAction,
                           externalObjectDirectoryRepository.findByTranscriptionDocumentEntityAndExternalLocationTypeAndStatus(transcription,
                                                                                                                               EodHelper.armLocation(),
                                                                                                                               EodHelper.storedStatus()));
            logApi.transcriptionDeleted(transcription.getId());
        }


        void processArmEods(OffsetDateTime deletedTs, ObjectAdminActionEntity objectAdminAction,
                            List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities) {
            externalObjectDirectoryEntities
                .forEach(externalObjectDirectoryEntity -> processArmEod(deletedTs, objectAdminAction, externalObjectDirectoryEntity));
        }

        void processArmEod(OffsetDateTime deletedTs, ObjectAdminActionEntity objectAdminAction,
                           ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
            armDataManagementApi.updateMetadata(
                externalObjectDirectoryEntity.getExternalRecordId(),
                deletedTs.minusYears(eventDateAdjustmentYears),
                getRetConfReason(deletedTs, objectAdminAction)
            );
            externalObjectDirectoryRepository.delete(externalObjectDirectoryEntity);
        }

        String getRetConfReason(OffsetDateTime deletedTs, ObjectAdminActionEntity objectAdminAction) {
            try {
                RetConfReason retConfReason = new RetConfReason();
                retConfReason.setManualDeletionTs(deletedTs);
                retConfReason.setManualDeletionReason(objectAdminAction.getObjectHiddenReason().getReason());
                retConfReason.setTicketReference(objectAdminAction.getTicketReference());
                retConfReason.setComments(objectAdminAction.getComments());
                return objectMapper.writeValueAsString(retConfReason);
            } catch (Exception e) {
                log.error("Error while creating RetConfReason", e);
                throw new DartsException("Error while creating RetConfReason", e);
            }
        }

        void deleteFromExternalDataStore(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
            if (ExternalLocationTypeEnum.INBOUND.getId().equals(externalObjectDirectoryEntity.getExternalLocationType().getId())) {
                inboundDeleter.delete(externalObjectDirectoryEntity);
            } else if (ExternalLocationTypeEnum.UNSTRUCTURED.getId().equals(externalObjectDirectoryEntity.getExternalLocationType().getId())) {
                unstructuredDeleter.delete(externalObjectDirectoryEntity);
            } else {
                log.error("Can only delete INBOUND (1) and UNSTRUCTURED (2) data but tried to delete : {}",
                          externalObjectDirectoryEntity.getExternalLocationType().getId());
            }
        }

        <T extends SoftDelete> boolean isNotDeleted(T softDelete) {
            return softDelete != null && !softDelete.isDeleted();
        }
    }
}
