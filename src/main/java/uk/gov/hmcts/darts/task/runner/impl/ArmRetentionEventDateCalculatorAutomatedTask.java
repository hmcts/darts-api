package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME;

@Slf4j
public class ArmRetentionEventDateCalculatorAutomatedTask extends AbstractLockableAutomatedTask {
    protected String taskName = ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME.getTaskName();
    private final ArmRetentionEventDateProcessor armRetentionEventDateProcessor;

    public ArmRetentionEventDateCalculatorAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                        LockProvider lockProvider,
                                                        AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                        ArmRetentionEventDateProcessor armRetentionEventDateProcessor,
                                                        LogApi logApi) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties, logApi);
        this.armRetentionEventDateProcessor = armRetentionEventDateProcessor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        armRetentionEventDateProcessor.calculateEventDates();
    }
}

