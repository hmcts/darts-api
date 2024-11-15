package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ArmMissingResponseReplayAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_MISSING_RESPONSE_REPLY_TASK_NAME;

@Slf4j
@Component
public class ArmMissingResponseReplayAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {
    protected ArmMissingResponseReplayAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                    ArmMissingResponseReplayAutomatedTaskConfig abstractAutomatedTaskConfig,
                                                    LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, abstractAutomatedTaskConfig, logApi, lockService);
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return ARM_MISSING_RESPONSE_REPLY_TASK_NAME;
    }

    @Override
    protected void runTask() {

    }
}
