package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryQueryTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnstructuredAudioDeleterProcessorImpl implements UnstructuredAudioDeleterProcessor {

    @Value("${darts.data-management.retention-period.unstructured.arm-minimum.weeks}")
    int weeksInArm;

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final UserAccountRepository userAccountRepository;
    private final SystemUserHelper systemUserHelper;

    private final EodHelper eodHelper;

    @Override
    public void markForDeletion() {

        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            weeksInArm,
            ChronoUnit.WEEKS
        );

        List<Integer> audioFileIdsToBeMarked = externalObjectDirectoryRepository.findIdsIn2StorageLocationsBeforeTime(
            EodHelper.storedStatus(),
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(),
            lastModifiedBefore,
            ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex()
        );

        if (audioFileIdsToBeMarked.isEmpty()) {
            log.debug("No Unstructured Audio files found that need to be marked for deletion.");
            return;
        }
        log.debug("Marking the following Unstructured ExternalObjectDirectory.Id's for deletion:- {}", audioFileIdsToBeMarked);

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