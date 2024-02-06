package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;


@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final LastAccessedDeletionDayCalculator deletionDayCalculator;
    private final SystemUserHelper systemUserHelper;
    private final TransformedMediaRepository transformedMediaRepository;

    @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}")
    private int deletionDays;

    @Transactional
    public List<TransientObjectDirectoryEntity> markForDeletion() {

        OffsetDateTime deletionStartDateTime = deletionDayCalculator.getStartDateForDeletion(deletionDays);

        List<TransformedMediaEntity> transformedMediaList = transformedMediaRepository.findAllDeletableTransformedMedia(
            deletionStartDateTime
        );

        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository
            .findByTransformedMediaIdIn(transformedMediaList.stream().map(TransformedMediaEntity::getId)
                                            .collect(Collectors.toList()));

        UserAccountEntity systemUser = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid(
            "housekeeping"));

        if (systemUser == null) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }
        ObjectRecordStatusEntity deletionStatus = objectRecordStatusRepository.getReferenceById(
            MARKED_FOR_DELETION.getId());

        List<TransientObjectDirectoryEntity> deletedValues = new ArrayList<>();
        for (TransientObjectDirectoryEntity entity : transientObjectDirectoryEntities) {
            TransformedMediaEntity transformedMedia = entity.getTransformedMedia();
            transformedMedia.setExpiryTime(OffsetDateTime.now());
            transformedMedia.setLastModifiedBy(systemUser);
            markTransientObjectDirectoryAsDeleted(entity, systemUser, deletionStatus);
            deletedValues.add(entity);
        }

        for (TransformedMediaEntity tm : transformedMediaList) {
            markMediaRequestAsExpired(tm.getMediaRequest(), systemUser);
        }

        return deletedValues;
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

    private void markMediaRequestAsExpired(MediaRequestEntity mediaRequest, UserAccountEntity systemUser) {
        List<TransformedMediaEntity> transformedMedias = transformedMediaRepository.findByMediaRequestId(mediaRequest.getId());
        boolean areAllTransformedMediasExpired = transformedMedias.stream().allMatch(t -> t.getExpiryTime() != null);
        if (areAllTransformedMediasExpired) {
            mediaRequest.setLastModifiedBy(systemUser);
            mediaRequest.setStatus(MediaRequestStatus.EXPIRED);
        }
    }

    @Override
    public void setDeletionDays(int deletionDays) {
        this.deletionDays = deletionDays;
    }
}
