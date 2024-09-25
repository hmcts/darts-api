package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.DetsToArmProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.DETS_TO_ARM_TASK_NAME;

@Slf4j
@Component
public class DetsToArmAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final DetsToArmProcessor detsToArmProcessor;

    @Autowired
    public DetsToArmAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                  AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                  DetsToArmProcessor processor,
                                  LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.detsToArmProcessor = processor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return DETS_TO_ARM_TASK_NAME;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(getTaskName());
        detsToArmProcessor.processDetsToArm(batchSize);

    }
}