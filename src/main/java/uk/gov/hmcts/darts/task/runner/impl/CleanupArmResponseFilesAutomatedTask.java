package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
@Component
public class CleanupArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {
    private final CleanupArmResponseFilesService cleanupArmResponseFilesService;

    @Autowired
    public CleanupArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                CleanupArmResponseFilesService cleanupArmResponseFilesService,
                                                LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.cleanupArmResponseFilesService = cleanupArmResponseFilesService;
    }

    @Override
    protected void runTask() {
        cleanupArmResponseFilesService.cleanupResponseFiles();
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;
    }

    @Override
    public Duration getLockAtMostFor() {
        return Duration.ofMinutes(90);
    }
}
