package uk.gov.hmcts.darts.task.runner.impl;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.service.AudioDiscoverySpikeService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AudioSpikeCarAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

@Component
public class AudioSpikeCarAutomatedTask extends AbstractLockableAutomatedTask<AudioSpikeCarAutomatedTaskConfig>
    implements AutoloadingManualTask {
    private final AudioDiscoverySpikeService service;

    public AudioSpikeCarAutomatedTask(AutomatedTaskRepository repository, AudioSpikeCarAutomatedTaskConfig config,
                                   LogApi logApi, LockService lockService, AudioDiscoverySpikeService service) {
        super(repository, config, logApi, lockService);
        this.service = service;
    }

    @Override
    protected void runTask() {
        service.scan(false, true, getAutomatedTaskBatchSize());
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.AUDIO_SPIKE_CAR;
    }
}
