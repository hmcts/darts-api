package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.BatchCleanupArmResponseFilesService;
import uk.gov.hmcts.darts.arm.service.impl.DetsBatchCleanupArmResponseFilesServiceImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.DetsBatchCleanupArmResponseFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.DETS_CLEANUP_ARM_RESPONSE_FILES;

@Slf4j
@Component
public class DetsCleanupArmResponseFilesAutomatedTask 
    extends AbstractLockableAutomatedTask<DetsBatchCleanupArmResponseFilesAutomatedTaskConfig>
    implements AutoloadingManualTask {
    private final BatchCleanupArmResponseFilesService batchCleanupArmResponseFilesService;

    @Autowired
    public DetsCleanupArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                    DetsBatchCleanupArmResponseFilesAutomatedTaskConfig automatedTaskConfigurationProperties,
                                                    DetsBatchCleanupArmResponseFilesServiceImpl batchCleanupArmResponseFilesService,
                                                    LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.batchCleanupArmResponseFilesService = batchCleanupArmResponseFilesService;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return DETS_CLEANUP_ARM_RESPONSE_FILES;
    }

    @Override
    protected void runTask() {
        batchCleanupArmResponseFilesService.cleanupResponseFiles(getAutomatedTaskBatchSize());
    }
}
