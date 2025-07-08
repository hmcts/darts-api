package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
@Slf4j
//NOTE: When this class is edited it is important we manually test the outbound audio deletion process
//This is because lazy loading errors are not be detected in integration tests
public class OutboundAudioDeleterProcessorSingleElementImpl implements OutboundAudioDeleterProcessorSingleElement {

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final MediaRequestService mediaRequestService;

    @Override
    @Transactional
    public List<TransientObjectDirectoryEntity> markForDeletion(TransformedMediaEntity transformedMedia) {

        markTransformedMediaAsExpired(transformedMedia);

        ObjectRecordStatusEntity deletionStatus = EodHelper.markForDeletionStatus();
        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository.findByTransformedMediaId(
            transformedMedia.getId()
        );
        for (TransientObjectDirectoryEntity entity : transientObjectDirectoryEntities) {
            markTransientObjectDirectoryAsDeleted(entity, deletionStatus);
        }

        return transientObjectDirectoryEntities;
    }

    /**
     * Marks media request as expired if all transformed medias related to the request have an expiry time.
     *
     * @param mediaRequestId media request id to be marked as expired.
     */
    @Override
    @Transactional
    public void markMediaRequestAsExpired(int mediaRequestId) {
        List<TransformedMediaEntity> transformedMedias = transformedMediaRepository.findByMediaRequestId(mediaRequestId);
        boolean areAllTransformedMediasExpired = transformedMedias
            .stream()
            .map(TransformedMediaEntity::getExpiryTime)
            .allMatch(Objects::nonNull);

        if (areAllTransformedMediasExpired) {
            log.debug("Setting media request ID {} to be expired", mediaRequestId);
            MediaRequestEntity mediaRequest = mediaRequestService.getMediaRequestEntityById(mediaRequestId);
            mediaRequest.setStatus(MediaRequestStatus.EXPIRED);

            mediaRequestRepository.saveAndFlush(mediaRequest);
        } else {
            log.debug("Not all transformed media for media request ID {} have an expiry date set", mediaRequestId);
        }
    }

    private void markTransformedMediaAsExpired(TransformedMediaEntity transformedMedia) {
        OffsetDateTime expiryTime = currentTimeHelper.currentOffsetDateTime();
        log.debug("Updating transformed media id {} expiry time to {}", transformedMedia.getId(), expiryTime);
        transformedMedia.setExpiryTime(expiryTime);
        transformedMediaRepository.saveAndFlush(transformedMedia);
    }

    private void markTransientObjectDirectoryAsDeleted(TransientObjectDirectoryEntity entity,
                                                       ObjectRecordStatusEntity deletionStatus) {
        log.debug("Updating transient object directory {} to be  deleted", entity.getId());
        entity.setStatus(deletionStatus);
    }
}
