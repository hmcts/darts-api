package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;

@Slf4j
public class CleanupArmResponseFilesAutomatedTask extends AbstractLockableAutomatedTask {
    protected String taskName = CLEANUP_ARM_RESPONSE_FILES_TASK_NAME.getTaskName();
    private final CleanupArmResponseFilesService cleanupArmResponseFilesService;

    public CleanupArmResponseFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                                LockProvider lockProvider,
                                                AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                                CleanupArmResponseFilesService cleanupArmResponseFilesService) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.cleanupArmResponseFilesService = cleanupArmResponseFilesService;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        cleanupArmResponseFilesService.cleanupResponseFiles();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
