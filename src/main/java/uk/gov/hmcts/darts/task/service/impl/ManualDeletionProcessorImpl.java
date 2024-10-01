package uk.gov.hmcts.darts.task.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.task.service.ManualDeletionProcessor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualDeletionProcessorImpl implements ManualDeletionProcessor {

    @Value("${darts.manual-deletion.grace-period:24h}")
    private String gracePeriod;

    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final MediaRepository mediaRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final ExternalInboundDataStoreDeleter inboundDeleter;
    private final ExternalUnstructuredDataStoreDeleter unstructuredDeleter;

    @Override
    public void process() {
        OffsetDateTime deletionThreshold = getDeletionThreshold();
        List<ObjectAdminActionEntity> actionsToDelete = objectAdminActionRepository.findFilesForManualDeletion(deletionThreshold);

        for (ObjectAdminActionEntity objectAdminAction : actionsToDelete) {
            if (isMediaNotDeleted(objectAdminAction)) {
                deleteMedia(objectAdminAction.getMedia());
            } else if (isTranscriptionNotDeleted(objectAdminAction)) {
                deleteTranscriptionDocument(objectAdminAction.getTranscriptionDocument());
            }
        }

    }

    private void deleteMedia(MediaEntity mediaEntity) {

        externalObjectDirectoryRepository.findByMediaStatusAndLocation(mediaEntity.getId())
            .stream()
            .peek(this::deleteFromExternalDataStore)
            .forEach(externalObjectDirectoryRepository::delete);

        mediaEntity.setDeleted(true);
        mediaRepository.save(mediaEntity);
        log.info("Deleted mediaEntity with ID: {}", mediaEntity.getId());
    }

    private void deleteTranscriptionDocument(TranscriptionDocumentEntity transcription) {
        externalObjectDirectoryRepository.findByTranscriptionStatusAndLocation(transcription.getId())
            .stream()
            .peek(this::deleteFromExternalDataStore)
            .forEach(externalObjectDirectoryRepository::delete);

        transcription.setDeleted(true);
        transcriptionDocumentRepository.save(transcription);
        log.info("Deleted transcription document with ID: {}", transcription.getId());
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
