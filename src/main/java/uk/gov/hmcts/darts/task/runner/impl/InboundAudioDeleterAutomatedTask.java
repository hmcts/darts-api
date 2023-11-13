package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.audio.service.InboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.INBOUND_AUDIO_DELETER_TASK_NAME;

@Slf4j
public class InboundAudioDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    protected String taskName = INBOUND_AUDIO_DELETER_TASK_NAME.getTaskName();
    private final InboundAudioDeleterProcessor inboundAudioDeleterProcessor;

    public InboundAudioDeleterAutomatedTask(AutomatedTaskRepository automatedTaskRepository, LockProvider lockProvider,
                                            AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                            InboundAudioDeleterProcessor processor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.inboundAudioDeleterProcessor = processor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        inboundAudioDeleterProcessor.markForDeletion();
        ;
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
