package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.RemoveRpoProductionsService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.RemoveRpoProductionsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Slf4j
@Component
public class RemoveOldArmRpoProductionsAutomatedTask
    extends AbstractLockableAutomatedTask<RemoveRpoProductionsAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final RemoveRpoProductionsService removeRpoProductionsService;

    public RemoveOldArmRpoProductionsAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                   RemoveRpoProductionsService removeRpoProductionsService,
                                                   RemoveRpoProductionsAutomatedTaskConfig abstractAutomatedTaskConfig, LogApi logApi,
                                                   LockService lockService) {
        super(automatedTaskRepository, abstractAutomatedTaskConfig, logApi, lockService);
        this. removeRpoProductionsService = removeRpoProductionsService;
    }

    @Override
    protected void runTask() {
        removeRpoProductionsService.removeOldArmRpoProductions(
            isManualRun(), getConfig().getWaitDuration(), getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.REMOVE_OLD_ARM_RPO_PRODUCTIONS;
    }
}
