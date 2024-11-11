package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmBatchProcessor;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.UnstructuredToArmAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.UNSTRUCTURED_TO_ARM_TASK_NAME;

@Slf4j
@Component
public class UnstructuredToArmAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final UnstructuredToArmBatchProcessor unstructuredToArmBatchProcessor;
    private final UnstructuredToArmProcessor unstructuredToArmProcessor;

    @Autowired
    public UnstructuredToArmAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                          UnstructuredToArmAutomatedTaskConfig automatedTaskConfigurationProperties,
                                          UnstructuredToArmBatchProcessor unstructuredToArmBatchProcessor,
                                          UnstructuredToArmProcessor unstructuredToArmProcessor,
                                          LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.unstructuredToArmBatchProcessor = unstructuredToArmBatchProcessor;
        this.unstructuredToArmProcessor = unstructuredToArmProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return UNSTRUCTURED_TO_ARM_TASK_NAME;
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(getTaskName());
        if (batchSize > 0) {
            unstructuredToArmBatchProcessor.processUnstructuredToArm(batchSize);
        } else {
            unstructuredToArmProcessor.processUnstructuredToArm();
        }
    }
}
