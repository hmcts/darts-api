package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.PROCESS_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
@Component
public class ProcessArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {
    private final AutomatedTaskProcessorFactory automatedTaskProcessorFactory;

    @Autowired
    public ProcessArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                AutomatedTaskProcessorFactory automatedTaskProcessorFactory,
                                                LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.automatedTaskProcessorFactory = automatedTaskProcessorFactory;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return PROCESS_ARM_RESPONSE_FILES_TASK_NAME;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofMinutes(90);
    }

    @Override
    protected void runTask() {
        Integer batchSize = getAutomatedTaskBatchSize(getTaskName());
        ArmResponseFilesProcessor armResponseFilesProcessor = automatedTaskProcessorFactory.createArmResponseFilesProcessor(batchSize);
        armResponseFilesProcessor.processResponseFiles();
    }
}
