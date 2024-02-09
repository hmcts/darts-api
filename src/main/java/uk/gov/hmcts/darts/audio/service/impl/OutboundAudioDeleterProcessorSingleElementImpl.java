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
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;


@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundAudioDeleterProcessorSingleElementImpl implements OutboundAudioDeleterProcessorSingleElement {

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;

    @Override
    @Transactional
    public List<TransientObjectDirectoryEntity> markForDeletion(UserAccountEntity userAccount,
                                                          TransformedMediaEntity transformedMedia) {

        ObjectRecordStatusEntity deletionStatus = objectRecordStatusRepository.getReferenceById(MARKED_FOR_DELETION.getId());
        //TODO verify these changes are propagated to DB if integration test is not transactional. Are these detached at this point?
        transformedMedia.setExpiryTime(OffsetDateTime.now());
        transformedMedia.setLastModifiedBy(userAccount);

        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository.findByTransformedMediaId(
            transformedMedia.getId()
        );

        for (TransientObjectDirectoryEntity entity : transientObjectDirectoryEntities) {
            markTransientObjectDirectoryAsDeleted(entity, userAccount, deletionStatus);
        }

        return transientObjectDirectoryEntities;
    }

    private void markTransientObjectDirectoryAsDeleted(TransientObjectDirectoryEntity entity, UserAccountEntity systemUser,
                                                       ObjectRecordStatusEntity deletionStatus) {
        entity.setLastModifiedBy(systemUser);
        entity.setStatus(deletionStatus);
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
            mediaRequest.setLastModifiedBy(userAccount);
            mediaRequest.setStatus(MediaRequestStatus.EXPIRED);
        }
    }
}
