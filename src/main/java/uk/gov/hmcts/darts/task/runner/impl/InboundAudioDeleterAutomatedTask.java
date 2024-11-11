package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.InboundAudioDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.INBOUND_AUDIO_DELETER_TASK_NAME;

@Slf4j
@Component
public class InboundAudioDeleterAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final InboundAudioDeleterProcessor inboundAudioDeleterProcessor;

    @Value("${darts.automated.task.inbound-audio-deleter.default-batch-size}")
    int defaultBatchSize;

    @Autowired
    public InboundAudioDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                            InboundAudioDeleterAutomatedTaskConfig automatedTaskConfigurationProperties,
                                            InboundAudioDeleterProcessor processor,
                                            LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.inboundAudioDeleterProcessor = processor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return INBOUND_AUDIO_DELETER_TASK_NAME;
    }

    @Override
    protected void runTask() {
        inboundAudioDeleterProcessor.markForDeletion(getAutomatedTaskBatchSize(defaultBatchSize));
    }
}
