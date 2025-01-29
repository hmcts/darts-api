package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.ArmRpoReplayService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;

import java.util.List;

@Service
@Slf4j
public class ArmRpoReplayServiceImpl implements ArmRpoReplayService {
    private final AutomatedTaskService automatedTaskService;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;

    public ArmRpoReplayServiceImpl(@Lazy AutomatedTaskService automatedTaskService,
                                   ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                   UserIdentity userIdentity,
                                   CurrentTimeHelper currentTimeHelper) {
        this.automatedTaskService = automatedTaskService;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.userIdentity = userIdentity;
        this.currentTimeHelper = currentTimeHelper;
    }

    @Transactional
    @Override
    public void replayArmRpo(int batchSize) {
        ArmAutomatedTaskEntity armAutomatedTaskEntity = automatedTaskService.getArmAutomatedTaskEntity(AutomatedTaskName.PROCESS_E2E_ARM_PENDING_TASK_NAME);
        List<Integer> eodIdsToBeUpdated = externalObjectDirectoryRepository.findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(
            EodHelper.armRpoPendingStatus(),
            armAutomatedTaskEntity.getArmReplayStartTs(),
            armAutomatedTaskEntity.getArmReplayEndTs(),
            EodHelper.armLocation(),
            Limit.of(batchSize)
        );

        if (eodIdsToBeUpdated.isEmpty()) {
            log.info("No EODs found to replay");
            return;
        }

        log.info("Found {} EODs to replay - {}", eodIdsToBeUpdated.size(), eodIdsToBeUpdated);

        externalObjectDirectoryRepository.updateStatus(EodHelper.failedArmRawDataStatus(),
                                                       userIdentity.getUserAccount(),
                                                       eodIdsToBeUpdated,
                                                       currentTimeHelper.currentOffsetDateTime());

    }
}
