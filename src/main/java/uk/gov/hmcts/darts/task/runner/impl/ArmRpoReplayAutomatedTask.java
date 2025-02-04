package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.ArmRpoReplayService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ArmRpoReplayAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME;

@Slf4j
@Component
public class ArmRpoReplayAutomatedTask extends AbstractLockableAutomatedTask<ArmRpoReplayAutomatedTaskConfig> implements AutoloadingManualTask {

    private final ArmRpoReplayService armRpoReplayService;

    protected ArmRpoReplayAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        ArmRpoReplayAutomatedTaskConfig automatedTaskConfig,
                                        LogApi logApi, LockService lockService,
                                        ArmRpoReplayService armRpoReplayService) {
        super(automatedTaskRepository, automatedTaskConfig, logApi, lockService);
        this.armRpoReplayService = armRpoReplayService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return ARM_RPO_REPLAY_TASK_NAME;
    }

    @Override
    public void run(boolean isManualRun) {
        super.run(isManualRun);
    }

    @Override
    protected void runTask() {
        armRpoReplayService.replayArmRpo(getAutomatedTaskBatchSize());
    }
}
