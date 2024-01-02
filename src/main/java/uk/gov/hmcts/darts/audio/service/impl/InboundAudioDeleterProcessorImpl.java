package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundAudioDeleterProcessorImpl implements InboundAudioDeleterProcessor {
    private final UserAccountRepository userAccountRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final SystemUserHelper systemUserHelper;

    @Value("${darts.data-management.retention-period.inbound.arm-minimum}")
    int hoursInArm;

    @Transactional
    public void markForDeletion() {
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(
            ObjectRecordStatusEnum.STORED.getId());
        ExternalLocationTypeEntity inboundLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.INBOUND.getId());
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.ARM.getId());
        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            hoursInArm,
            ChronoUnit.HOURS
        );
        List<Integer> audioFileIdsToBeMarked = externalObjectDirectoryRepository.findMediaFileIdsIn2StorageLocationsBeforeTime(
            storedStatus,
            storedStatus,
            inboundLocation,
            armLocation,
            lastModifiedBefore
        );

        if (audioFileIdsToBeMarked.isEmpty()) {
            log.debug("No Inbound Audio files found that need to be marked for deletion.");
            return;
        }
        log.debug("Marking the following ExternalObjectDirectory.Id's for deletion:- {}", audioFileIdsToBeMarked);

        ObjectRecordStatusEntity deletionStatus = objectRecordStatusRepository.getReferenceById(
            MARKED_FOR_DELETION.getId());

        UserAccountEntity user = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid("housekeeping"));
        externalObjectDirectoryRepository.updateStatus(
            deletionStatus,
            user,
            audioFileIdsToBeMarked,
            OffsetDateTime.now()
        );
    }
}
