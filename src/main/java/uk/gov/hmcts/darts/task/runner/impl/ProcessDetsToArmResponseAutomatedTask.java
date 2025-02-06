package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.impl.DetsToArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.ProcessDetsToArmResponseAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.PROCESS_DETS_TO_ARM_RESPONSE;

@Slf4j
@Component
public class ProcessDetsToArmResponseAutomatedTask 
    extends AbstractLockableAutomatedTask<ProcessDetsToArmResponseAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final DetsToArmBatchProcessResponseFilesImpl detsToArmBatchProcessResponseFiles;

    @Autowired
    public ProcessDetsToArmResponseAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                 ProcessDetsToArmResponseAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                 LogApi logApi, LockService lockService,
                                                 DetsToArmBatchProcessResponseFilesImpl detsToArmBatchProcessResponseFiles) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.detsToArmBatchProcessResponseFiles = detsToArmBatchProcessResponseFiles;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return PROCESS_DETS_TO_ARM_RESPONSE;
    }

    @Override
    protected void runTask() {
        detsToArmBatchProcessResponseFiles.processResponseFiles(getAutomatedTaskBatchSize(), getConfig());
    }
}