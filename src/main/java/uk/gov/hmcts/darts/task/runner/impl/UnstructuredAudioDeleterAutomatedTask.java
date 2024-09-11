package uk.gov.hmcts.darts.task.runner.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.service.UnstructuredAudioDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.UNSTRUCTURED_AUDIO_DELETER_TASK_NAME;

@Slf4j
@Component
public class UnstructuredAudioDeleterAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    protected String taskName = UNSTRUCTURED_AUDIO_DELETER_TASK_NAME.getTaskName();

    private final UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor;

    public UnstructuredAudioDeleterAutomatedTask(
        AutomatedTaskRepository automatedTaskRepository,
        AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
        UnstructuredAudioDeleterProcessor unstructuredAudioDeleterProcessor,
        LogApi logApi, LockService lockService) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
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
}
