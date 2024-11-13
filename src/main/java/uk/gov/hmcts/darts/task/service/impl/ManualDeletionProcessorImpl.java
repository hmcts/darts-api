package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.ManualDeletionProcessor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualDeletionProcessorImpl implements ManualDeletionProcessor {

    private final UserIdentity userIdentity;
    @Value("${darts.manual-deletion.grace-period:24h}")
    private String gracePeriod;

    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final MediaRepository mediaRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;
    private final LogApi logApi;

    @Override
    public void process(Integer batchSize) {
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        OffsetDateTime deletionThreshold = getDeletionThreshold();
        List<ObjectAdminActionEntity> actionsToDelete = objectAdminActionRepository.findFilesForManualDeletion(deletionThreshold, Limit.of(batchSize));

        for (ObjectAdminActionEntity objectAdminAction : actionsToDelete) {
            if (isMediaNotDeleted(objectAdminAction)) {
                deleteMedia(userAccount, objectAdminAction.getMedia());
            } else if (isTranscriptionNotDeleted(objectAdminAction)) {
                deleteTranscriptionDocument(userAccount, objectAdminAction.getTranscriptionDocument());
            }
        }

    }

    private void deleteMedia(UserAccountEntity userAccount, MediaEntity mediaEntity) {
        log.info("Deleting mediaEntity with ID: {}", mediaEntity.getId());
        List<ExternalObjectDirectoryEntity> objectsToDelete = externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByMediaId(
            mediaEntity.getId());

        for (ExternalObjectDirectoryEntity externalObjectDirectoryEntity : objectsToDelete) {
            deleteFromExternalDataStore(externalObjectDirectoryEntity);
            externalObjectDirectoryRepository.delete(externalObjectDirectoryEntity);
        }

        mediaEntity.markAsDeleted(userAccount);
        mediaRepository.save(mediaEntity);
        logApi.mediaDeleted(mediaEntity.getId());
    }

    private void deleteTranscriptionDocument(UserAccountEntity userAccount, TranscriptionDocumentEntity transcription) {
        log.info("Deleting transcription document with ID: {}", transcription.getId());

        List<ExternalObjectDirectoryEntity> objectsToDelete =
            externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByTranscriptionId(transcription.getId());

        for (ExternalObjectDirectoryEntity externalObjectDirectoryEntity : objectsToDelete) {
            deleteFromExternalDataStore(externalObjectDirectoryEntity);
            externalObjectDirectoryRepository.delete(externalObjectDirectoryEntity);
        }

        transcription.markAsDeleted(userAccount);
        transcriptionDocumentRepository.save(transcription);
        logApi.transcriptionDeleted(transcription.getId());
    }

    private void deleteFromExternalDataStore(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        if (ExternalLocationTypeEnum.INBOUND.getId().equals(externalObjectDirectoryEntity.getExternalLocationType().getId())) {
            inboundDeleter.delete(externalObjectDirectoryEntity);
        } else if (ExternalLocationTypeEnum.UNSTRUCTURED.getId().equals(externalObjectDirectoryEntity.getExternalLocationType().getId())) {
            unstructuredDeleter.delete(externalObjectDirectoryEntity);
        } else {
            log.error("Can only delete INBOUND (1) and UNSTRUCTURED (2) data but tried to delete : {}",
                      externalObjectDirectoryEntity.getExternalLocationType().getId());
        }
    }

    private boolean isTranscriptionNotDeleted(ObjectAdminActionEntity action) {
        return action.getTranscriptionDocument() != null && !action.getTranscriptionDocument().isDeleted();
    }

    private boolean isMediaNotDeleted(ObjectAdminActionEntity action) {
        return action.getMedia() != null && !action.getMedia().isDeleted();
    }

    public OffsetDateTime getDeletionThreshold() {
        return OffsetDateTime.now().minus(Duration.parse("PT" + gracePeriod));
    }
}
