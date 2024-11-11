package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ArmRetentionEventDateCalculatorAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME;

@Slf4j
@Component
public class ArmRetentionEventDateCalculatorAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {
    private final ArmRetentionEventDateProcessor armRetentionEventDateProcessor;

    @Autowired
    public ArmRetentionEventDateCalculatorAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                        ArmRetentionEventDateCalculatorAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                        ArmRetentionEventDateProcessor armRetentionEventDateProcessor,
                                                        LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.armRetentionEventDateProcessor = armRetentionEventDateProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME;
    }

    @Override
    protected void runTask() {
        armRetentionEventDateProcessor.calculateEventDates(getAutomatedTaskBatchSize());
    }
}

