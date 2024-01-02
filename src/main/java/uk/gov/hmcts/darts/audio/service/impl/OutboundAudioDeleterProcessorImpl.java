package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;

@Service
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final MediaRequestRepository mediaRequestRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final LastAccessedDeletionDayCalculator deletionDayCalculator;
    private final SystemUserHelper systemUserHelper;

    public OutboundAudioDeleterProcessorImpl(MediaRequestRepository mediaRequestRepository,
                                             TransientObjectDirectoryRepository transientObjectDirectoryRepository,
                                             UserAccountRepository userAccountRepository,
                                             ObjectRecordStatusRepository objectRecordStatusRepository,
                                             LastAccessedDeletionDayCalculator deletionDayCalculator,
                                             SystemUserHelper systemUserHelper) {
        this.mediaRequestRepository = mediaRequestRepository;
        this.transientObjectDirectoryRepository = transientObjectDirectoryRepository;
        this.userAccountRepository = userAccountRepository;
        this.objectRecordStatusRepository = objectRecordStatusRepository;
        this.deletionDayCalculator = deletionDayCalculator;
        this.systemUserHelper = systemUserHelper;
    }


    @Transactional
    public List<MediaRequestEntity> markForDeletion() {
        List<MediaRequestEntity> deletedValues = new ArrayList<>();
        OffsetDateTime deletionStartDateTime = this.deletionDayCalculator.getStartDateForDeletion();

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
            entity.getTransformedMedia().getMediaRequest().setStatus(MediaRequestStatus.EXPIRED);
            entity.getTransformedMedia().getMediaRequest().setLastModifiedBy(systemUser);

            entity.setLastModifiedBy(systemUser);
            entity.setStatus(deletionStatus);

            deletedValues.add(entity.getTransformedMedia().getMediaRequest());
        }

        return deletedValues;
    }
}
