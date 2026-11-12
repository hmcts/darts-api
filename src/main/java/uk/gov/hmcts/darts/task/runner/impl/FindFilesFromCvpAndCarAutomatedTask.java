package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.service.FindFilesFromCvpAndCarService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.FindFilesFromCvpAndCarAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Slf4j
@Component
public class FindFilesFromCvpAndCarAutomatedTask extends AbstractLockableAutomatedTask<FindFilesFromCvpAndCarAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final FindFilesFromCvpAndCarService findFilesFromCvpAndCarService;

    public FindFilesFromCvpAndCarAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                               FindFilesFromCvpAndCarAutomatedTaskConfig config,
                                               LogApi logApi, LockService lockService,
                                               FindFilesFromCvpAndCarService findFilesFromCvpAndCarService) {
        super(automatedTaskRepository, config, logApi, lockService);
        this.findFilesFromCvpAndCarService = findFilesFromCvpAndCarService;
    }

    @Override
    protected void runTask() {
        findFilesFromCvpAndCarService.findFilesFromCvpAndCar(getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.FIND_FILES_FROM_CVP_AND_CAR_TASK_NAME;
    }
}
