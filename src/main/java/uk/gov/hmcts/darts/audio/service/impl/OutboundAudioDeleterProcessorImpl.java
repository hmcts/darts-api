package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.exception.OutboundDeleterException;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.MARKED_FOR_DELETION;

@Service
public class OutboundAudioDeleterProcessorImpl implements OutboundAudioDeleterProcessor {
    private final MediaRequestRepository mediaRequestRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final LastAccessedDeletionDayCalculator deletionDayCalculator;
    private final SystemUserHelper systemUserHelper;

    public OutboundAudioDeleterProcessorImpl(MediaRequestRepository mediaRequestRepository,
                                             TransientObjectDirectoryRepository transientObjectDirectoryRepository,
                                             UserAccountRepository userAccountRepository,
                                             ObjectDirectoryStatusRepository objectDirectoryStatusRepository,
                                             LastAccessedDeletionDayCalculator deletionDayCalculator,
                                             SystemUserHelper systemUserHelper) {
        this.mediaRequestRepository = mediaRequestRepository;
        this.transientObjectDirectoryRepository = transientObjectDirectoryRepository;
        this.userAccountRepository = userAccountRepository;
        this.objectDirectoryStatusRepository = objectDirectoryStatusRepository;
        this.deletionDayCalculator = deletionDayCalculator;
        this.systemUserHelper = systemUserHelper;
    }


    @Transactional
    public List<MediaRequestEntity> delete() {
        List<MediaRequestEntity> deletedValues = new ArrayList<>();
        OffsetDateTime deletionStartDateTime = this.deletionDayCalculator.getStartDateForDeletion();

        List<Integer> mediaRequests = mediaRequestRepository.findAllIdsByLastAccessedTimeBeforeAndStatus(
            deletionStartDateTime,
            AudioRequestStatus.COMPLETED
        );

        mediaRequests.addAll(mediaRequestRepository.findAllByCreatedDateTimeBeforeAndStatusNotAndLastAccessedDateTimeIsNull(
            deletionStartDateTime,
            AudioRequestStatus.PROCESSING
        ));

        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository.findByMediaRequest_idIn(
            mediaRequests);

        UserAccountEntity systemUser = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid(
            "housekeeping"));

        if (systemUser == null) {
            throw new DartsApiException(OutboundDeleterException.MISSING_SYSTEM_USER);
        }
        ObjectDirectoryStatusEntity deletionStatus = objectDirectoryStatusRepository.getReferenceById(
            MARKED_FOR_DELETION.getId());


        for (TransientObjectDirectoryEntity entity : transientObjectDirectoryEntities) {
            entity.getMediaRequest().setStatus(AudioRequestStatus.EXPIRED);
            entity.getMediaRequest().setLastModifiedBy(systemUser);

            entity.setLastModifiedBy(systemUser);
            entity.setStatus(deletionStatus);

            deletedValues.add(entity.getMediaRequest());
        }

        return deletedValues;
    }
}
