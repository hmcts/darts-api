package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.MediaRequestCleanUpAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.OffsetDateTime;

@Slf4j
@Component
public class MediaRequestCleanUpAutomatedTask
    extends AbstractLockableAutomatedTask<MediaRequestCleanUpAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final CurrentTimeHelper currentTimeHelper;
    private final MediaRequestRepository mediaRequestRepository;

    protected MediaRequestCleanUpAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                               MediaRequestCleanUpAutomatedTaskConfig config, LogApi logApi,
                                               LockService lockService,
                                               CurrentTimeHelper currentTimeHelper,
                                               MediaRequestRepository mediaRequestRepository) {
        super(automatedTaskRepository, config, logApi, lockService);
        this.currentTimeHelper = currentTimeHelper;
        this.mediaRequestRepository = mediaRequestRepository;
    }

    @Override
    protected void runTask() {
        OffsetDateTime currentTime = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime maxStuckTime = currentTime.minus(getConfig().getMaxStuckDuration());
        mediaRequestRepository.cleanupStuckRequests(maxStuckTime);
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.MEDIA_REQUEST_CLEANUP;
    }
}
