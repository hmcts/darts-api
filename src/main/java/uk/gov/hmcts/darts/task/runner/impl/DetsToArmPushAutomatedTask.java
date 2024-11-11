package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.DetsToArmBatchPushProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.DetsToArmPushAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.DETS_TO_ARM_TASK_NAME;

@Slf4j
@Component
public class DetsToArmPushAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final DetsToArmBatchPushProcessor detsToArmPushProcessor;

    @Autowired
    public DetsToArmPushAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                      DetsToArmPushAutomatedTaskConfig automatedTaskConfigurationProperties,
                                      DetsToArmBatchPushProcessor processor,
                                      LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.detsToArmPushProcessor = processor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return DETS_TO_ARM_TASK_NAME;
    }

    @Override
    protected void runTask() {
        detsToArmPushProcessor.processDetsToArm(getAutomatedTaskBatchSize());
    }
}