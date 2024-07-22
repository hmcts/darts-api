package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.INBOUND_AUDIO_DELETER_TASK_NAME;

@Slf4j
public class InboundAudioDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = INBOUND_AUDIO_DELETER_TASK_NAME.getTaskName();
    private final InboundAudioDeleterProcessor inboundAudioDeleterProcessor;

    public InboundAudioDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                            AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                            InboundAudioDeleterProcessor processor,
                                            LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.inboundAudioDeleterProcessor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        inboundAudioDeleterProcessor.markForDeletion();
    }
}
