package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;

@Service
@RequiredArgsConstructor
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final MediaRequestRepository mediaRequestRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final LastAccessedDeletionDayCalculator deletionDayCalculator;
    private final SystemUserHelper systemUserHelper;
    private final TransformedMediaRepository transformedMediaRepository;

    @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}")
    private int deletionDays;

    @Transactional
    public List<MediaRequestEntity> markForDeletion() {
        List<MediaRequestEntity> deletedValues = new ArrayList<>();
        OffsetDateTime deletionStartDateTime = deletionDayCalculator.getStartDateForDeletion(deletionDays);

        List<Integer> mediaRequests = mediaRequestRepository.findAllIdsByLastAccessedTimeBeforeAndStatus(
            deletionStartDateTime,
            MediaRequestStatus.COMPLETED
        );

        mediaRequests.addAll(mediaRequestRepository.findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(
            deletionStartDateTime,
            MediaRequestStatus.PROCESSING
        ));

        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository.findByMediaRequestIds(
            mediaRequests);

        UserAccountEntity systemUser = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid(
            "housekeeping"));

        if (systemUser == null) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }
        ObjectRecordStatusEntity deletionStatus = objectRecordStatusRepository.getReferenceById(
            MARKED_FOR_DELETION.getId());


        for (TransientObjectDirectoryEntity entity : transientObjectDirectoryEntities) {
            entity.getTransformedMedia().getMediaRequest().setLastModifiedBy(systemUser);

            entity.setLastModifiedBy(systemUser);
            entity.setStatus(deletionStatus);
            entity.getTransformedMedia().setExpiryTime(OffsetDateTime.now());
            deletedValues.add(entity.getTransformedMedia().getMediaRequest());
        }

        for (Integer mediaRequestId : mediaRequests) {
            List<TransformedMediaEntity> transformedMedias = transformedMediaRepository.findByMediaRequestId(mediaRequestId);
            MediaRequestEntity mediaRequest = mediaRequestRepository.findById(mediaRequestId).get();
            boolean areAllTransformedMediasExpired = transformedMedias.stream().allMatch(t -> t.getExpiryTime() != null);
            if (areAllTransformedMediasExpired) {
                mediaRequest.setStatus(MediaRequestStatus.EXPIRED);
            }
        }
        return deletedValues;
    }

    @Override
    public void setDeletionDays(int deletionDays) {
        this.deletionDays = deletionDays;
    }
}
