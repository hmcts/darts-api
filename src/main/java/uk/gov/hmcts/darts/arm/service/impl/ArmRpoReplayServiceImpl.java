package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.ArmRpoReplayService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;

import java.util.List;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME;

@Service
@Slf4j
public class ArmRpoReplayServiceImpl implements ArmRpoReplayService {
    private final AutomatedTaskService automatedTaskService;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final UserIdentity userIdentity;

    public ArmRpoReplayServiceImpl(@Lazy AutomatedTaskService automatedTaskService,
                                   ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                   UserIdentity userIdentity) {
        this.automatedTaskService = automatedTaskService;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.userIdentity = userIdentity;
    }

    @Transactional
    @Override
    public void replayArmRpo(int batchSize) {
        ArmAutomatedTaskEntity armAutomatedTaskEntity = automatedTaskService.getArmAutomatedTaskEntity(ARM_RPO_REPLAY_TASK_NAME);

        List<Long> eodIdsToBeUpdated = externalObjectDirectoryRepository.findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(
            EodHelper.armReplayStatus(),
            armAutomatedTaskEntity.getArmReplayStartTs(),
            armAutomatedTaskEntity.getArmReplayEndTs(),
            EodHelper.armLocation(),
            Limit.of(batchSize)
        );

        if (eodIdsToBeUpdated.isEmpty()) {
            log.info("No EODs found to replay");
            return;
        }

        log.info("Found {} EODs out of total batch size {} to replay - {}", eodIdsToBeUpdated.size(), batchSize, eodIdsToBeUpdated);

        externalObjectDirectoryRepository.updateEodStatusAndTransferAttemptsWhereIdIn(EodHelper.failedArmRawDataStatus(),
                                                                                      0,
                                                                                      userIdentity.getUserAccount().getId(),
                                                                                      eodIdsToBeUpdated);

    }
}
