package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.service.FindFilesService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.FindFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Slf4j
@Component
public class FindFilesAutomatedTask extends AbstractLockableAutomatedTask<FindFilesAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final FindFilesService findFilesService;

    public FindFilesAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                  FindFilesAutomatedTaskConfig config,
                                  LogApi logApi, LockService lockService,
                                  FindFilesService findFilesService) {
        super(automatedTaskRepository, config, logApi, lockService);
        this.findFilesService = findFilesService;
    }

    @Override
    protected void runTask() {
        findFilesService.findFiles(getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.FIND_FILES_TASK_NAME;
    }
}
