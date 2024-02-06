package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.UNSTRUCTURED_AUDIO_DELETER_TASK_NAME;

@Slf4j
public class UnstructuredAudioDeleterAutomatedTask extends AbstractLockableAutomatedTask {

    private final UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;
    protected String taskName = UNSTRUCTURED_AUDIO_DELETER_TASK_NAME.getTaskName();

    public UnstructuredAudioDeleterAutomatedTask(
          AutomatedTaskRepository automatedTaskRepository,
          LockProvider lockProvider,
          AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
          UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor) {
        super(automatedTaskRepository, lockProvider, automatedTaskConfigurationProperties);
        this.unstructuredAudioDeleterProcessor = unstructuredAudioDeleterProcessor;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    protected void runTask() {
        unstructuredAudioDeleterProcessor.markForDeletion();
    }

    @Override
    protected void handleException(Exception exception) {
        log.error("Exception: {}", exception.getMessage());
    }
}
