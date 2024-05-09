package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundAudioDeleterProcessorImpl implements InboundAudioDeleterProcessor {
    private final UserAccountRepository userAccountRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final SystemUserHelper systemUserHelper;
    private final EodHelper eodHelper;


    @Value("${darts.data-management.retention-period.inbound.arm-minimum}")
    int hoursInArm;

    @Override
    public void markForDeletion() {
        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            hoursInArm,
            ChronoUnit.HOURS
        );
        List<Integer> audioFileIdsToBeMarked = externalObjectDirectoryRepository.findMediaFileIdsIn2StorageLocationsBeforeTime(
            EodHelper.storedStatus(),
            EodHelper.storedStatus(),
            EodHelper.inboundLocation(),
            EodHelper.armLocation(),
            lastModifiedBefore
        );

        if (audioFileIdsToBeMarked.isEmpty()) {
            log.debug("No Inbound Audio files found that need to be marked for deletion.");
            return;
        }
        log.debug("Marking the following ExternalObjectDirectory.Id's for deletion:- {}", audioFileIdsToBeMarked);

        UserAccountEntity user = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid("housekeeping"));
        eodHelper.updateStatus(
            EodHelper.markForDeletionStatus(),
            user,
            audioFileIdsToBeMarked,
            OffsetDateTime.now()
        );
        audioFileIdsToBeMarked.stream().forEach(eodId -> log.info("Set status of EOD {} to be marked for deletion", eodId));

    }
}
