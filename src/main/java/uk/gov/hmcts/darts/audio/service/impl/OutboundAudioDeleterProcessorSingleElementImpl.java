package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundAudioDeleterProcessorSingleElementImpl implements OutboundAudioDeleterProcessorSingleElement {

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    @Transactional
    public List<TransientObjectDirectoryEntity> markForDeletion(UserAccountEntity userAccount, TransformedMediaEntity transformedMedia) {

        markTransformedMediaAsExpired(userAccount, transformedMedia);

        ObjectRecordStatusEntity deletionStatus = EodHelper.markForDeletionStatus();
        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository.findByTransformedMediaId(
            transformedMedia.getId()
        );
        for (TransientObjectDirectoryEntity entity : transientObjectDirectoryEntities) {
            markTransientObjectDirectoryAsDeleted(entity, userAccount, deletionStatus);
        }

        return transientObjectDirectoryEntities;
    }

    /**
     * Marks media request as expired if all transformed medias related to the request have an expiry time.
     *
     * @param mediaRequest media request to be marked as expired.
     */
    @Override
    @Transactional
    public void markMediaRequestAsExpired(MediaRequestEntity mediaRequest, UserAccountEntity userAccount) {

        List<TransformedMediaEntity> transformedMedias = transformedMediaRepository.findByMediaRequestId(mediaRequest.getId());
        boolean areAllTransformedMediasExpired = transformedMedias.stream().allMatch(t -> t.getExpiryTime() != null);
        if (areAllTransformedMediasExpired) {
            log.debug("Setting media request ID {} to be expired", mediaRequest.getId());
            mediaRequest.setLastModifiedBy(userAccount);
            mediaRequest.setStatus(MediaRequestStatus.EXPIRED);
            mediaRequestRepository.saveAndFlush(mediaRequest);
        } else {
            log.debug("Not all transformed media for media request ID {} have an expiry date set", mediaRequest.getId());
        }
    }

    private void markTransformedMediaAsExpired(UserAccountEntity userAccount, TransformedMediaEntity transformedMedia) {
        OffsetDateTime expiryTime = currentTimeHelper.currentOffsetDateTime();
        log.debug("Updating transformed media id {} expiry time to {}", transformedMedia.getId(), expiryTime);
        transformedMedia.setExpiryTime(expiryTime);
        transformedMedia.setLastModifiedBy(userAccount);
        transformedMediaRepository.saveAndFlush(transformedMedia);
    }

    private void markTransientObjectDirectoryAsDeleted(TransientObjectDirectoryEntity entity, UserAccountEntity systemUser,
                                                       ObjectRecordStatusEntity deletionStatus) {
        log.debug("Updating transient object directory {} to be  deleted", entity.getId());
        entity.setLastModifiedBy(systemUser);
        entity.setStatus(deletionStatus);
    }
}
