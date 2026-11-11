package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.SpikeDisableAndEnableInDeployedEnvironmentService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.SpikeDisableAndEnableInDeployedEnvironmentAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Component
public class SpikeDisableAndEnableInDeployedEnvironmentAutomatedTask
    extends AbstractLockableAutomatedTask<SpikeDisableAndEnableInDeployedEnvironmentAutomatedTaskConfig> implements AutoloadingManualTask {

    private final SpikeDisableAndEnableInDeployedEnvironmentService prototypeService;

    public SpikeDisableAndEnableInDeployedEnvironmentAutomatedTask(
        AutomatedTaskRepository automatedTaskRepository,
        SpikeDisableAndEnableInDeployedEnvironmentAutomatedTaskConfig automatedTaskConfig,
        LogApi logApi, LockService lockService, SpikeDisableAndEnableInDeployedEnvironmentService prototypeService) {
        super(automatedTaskRepository, automatedTaskConfig, logApi, lockService);
        this.prototypeService = prototypeService;
    }

    @Override
    protected void runTask() {
        prototypeService.process(getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.SPIKE_DISABLE_AND_ENABLE_IN_DEPLOYED_ENVIRONMENT_TASK_NAME;
    }
}
