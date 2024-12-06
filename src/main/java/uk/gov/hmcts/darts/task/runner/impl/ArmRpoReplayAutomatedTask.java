package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ArmRpoReplayAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME;

@Slf4j
@Component
public class ArmRpoReplayAutomatedTask
    extends AbstractLockableAutomatedTask<ArmRpoReplayAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final AutomatedTaskService automatedTaskService;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final UserIdentity userIdentity;

    protected ArmRpoReplayAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        ArmRpoReplayAutomatedTaskConfig automatedTaskConfig,
                                        LogApi logApi, LockService lockService,
                                        @Lazy AutomatedTaskService automatedTaskService,
                                        ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                        ObjectRecordStatusRepository objectRecordStatusRepository, UserIdentity userIdentity) {
        super(automatedTaskRepository, automatedTaskConfig, logApi, lockService);
        this.automatedTaskService = automatedTaskService;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.objectRecordStatusRepository = objectRecordStatusRepository;
        this.userIdentity = userIdentity;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return ARM_RPO_REPLAY_TASK_NAME;
    }

    @Override
    @Transactional
    public void run(boolean isManualRun) {
        super.run(isManualRun);
    }

    @Override
    protected void runTask() {
        ArmAutomatedTaskEntity armAutomatedTaskEntity = automatedTaskService.getArmAutomatedTaskEntity(AutomatedTaskName.PROCESS_E2E_ARM_PENDING_TASK_NAME);
        externalObjectDirectoryRepository.updateEodStatusAndTransferAttemptsWhereLastModifiedIsBetweenTwoDateTimesAndHasStatus(
            objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED.getId()),
            0,
            objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.ARM_REPLAY.getId()),
            armAutomatedTaskEntity.getArmReplayStartTs(),
            armAutomatedTaskEntity.getArmReplayEndTs(),
            userIdentity.getUserAccount()
        );
    }
}
