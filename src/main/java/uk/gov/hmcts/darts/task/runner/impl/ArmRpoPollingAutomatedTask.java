package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ArmRpoPollAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RPO_POLLING_TASK_NAME;

@ConditionalOnProperty(
    value = "darts.automated.task.process-e2e-arm-rpo",
    havingValue = "true"
)
@Slf4j
@Component
public class ArmRpoPollingAutomatedTask
    extends AbstractLockableAutomatedTask<ArmRpoPollAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final ArmRpoPollService armRpoPollService;

    @Autowired
    public ArmRpoPollingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                      ArmRpoPollAutomatedTaskConfig armRpoPollingAutomatedTaskConfig,
                                      ArmRpoPollService armRpoPollService, LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, armRpoPollingAutomatedTaskConfig, logApi, lockService);
        this.armRpoPollService = armRpoPollService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return ARM_RPO_POLLING_TASK_NAME;
    }

    @Override
    protected void runTask() {
        armRpoPollService.pollArmRpo(isManualRun());
    }
}
