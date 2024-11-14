package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RPO_POLL_TASK_NAME;

@Slf4j
@Component
public class ArmRpoPollAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final ArmRpoPollService armRpoPollService;

    public ArmRpoPollAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                   AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                   ArmRpoPollService armRpoPollService, LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.armRpoPollService = armRpoPollService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return ARM_RPO_POLL_TASK_NAME;
    }

    @Override
    protected void runTask() {
        armRpoPollService.pollArmRpo();
    }
}
