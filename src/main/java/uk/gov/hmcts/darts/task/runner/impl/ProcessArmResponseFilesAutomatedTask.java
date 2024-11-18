package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ProcessArmResponseFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.PROCESS_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
@Component
public class ProcessArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {
    private final ArmBatchProcessResponseFilesImpl armBatchProcessResponseFiles;

    @Autowired
    public ProcessArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                ProcessArmResponseFilesAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                LogApi logApi, LockService lockService,
                                                ArmBatchProcessResponseFilesImpl armBatchProcessResponseFiles) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.armBatchProcessResponseFiles = armBatchProcessResponseFiles;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return PROCESS_ARM_RESPONSE_FILES_TASK_NAME;
    }

    @Override
    protected void runTask() {
        armBatchProcessResponseFiles.processResponseFiles(getAutomatedTaskBatchSize());
    }
}
